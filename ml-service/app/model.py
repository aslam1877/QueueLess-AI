import os
import json
import math
import logging
from datetime import datetime
from typing import Optional, Dict, Any, Tuple
import joblib
import pandas as pd

from .schemas import PredictRequest, PredictResponse

logger = logging.getLogger("queueless.ml")

class WaitTimePredictor:
    """Wrapper around trained scikit-learn pipeline with fallback resilience."""

    def __init__(self, model_path: Optional[str] = None, metrics_path: Optional[str] = None):
        app_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.dirname(app_dir)

        self.model_path = model_path or os.path.join(project_root, 'models', 'wait_time_model.joblib')
        self.metrics_path = metrics_path or os.path.join(project_root, 'models', 'model_metrics.json')

        self.pipeline = None
        self.metrics: Dict[str, Any] = {}
        self.model_name = "Gradient Boosting Regressor"
        self.is_loaded = False

        self.load_model()

    def load_model(self) -> bool:
        """Loads the serialized model and metrics file from disk."""
        try:
            if os.path.exists(self.model_path):
                self.pipeline = joblib.load(self.model_path)
                self.is_loaded = True
                logger.info(f"Loaded ML model pipeline from {self.model_path}")
            else:
                logger.warning(f"Model file not found at {self.model_path}. Fallback mode active.")
                self.is_loaded = False

            if os.path.exists(self.metrics_path):
                with open(self.metrics_path, 'r') as f:
                    self.metrics = json.load(f)
                    self.model_name = self.metrics.get('model_name', self.model_name)
                logger.info(f"Loaded model metrics from {self.metrics_path}")
        except Exception as e:
            logger.error(f"Error loading model: {e}. Defaulting to baseline math fallback.", exc_info=True)
            self.pipeline = None
            self.is_loaded = False

        return self.is_loaded

    def calculate_baseline(self, people_ahead: int, service_duration: float, active_counters: int) -> int:
        """Deterministic baseline queueing math: ceil((peopleAhead * duration) / counters)."""
        if people_ahead <= 0:
            return 0
        counters = max(1, active_counters)
        duration = max(1.0, service_duration)
        return int(math.ceil((people_ahead * duration) / counters))

    def predict(self, req: PredictRequest) -> PredictResponse:
        """
        Predicts wait time using the trained ML model with non-linear factors.
        Guarantees response through graceful fallback if ML is unavailable.
        """
        now = datetime.now()
        hour = req.hour_of_day if req.hour_of_day is not None else now.hour
        day = req.day_of_week if req.day_of_week is not None else now.weekday()
        category = req.service_category or "ROUTINE_CHECKUP"

        is_weekend = 1 if day in (5, 6) else 0
        is_peak_hour = 1 if hour in (11, 12, 13, 17, 18) else 0

        baseline = self.calculate_baseline(req.people_ahead, req.service_duration_min, req.active_counters)

        # Edge case: next in line
        if req.people_ahead == 0:
            return PredictResponse(
                predicted_wait_minutes=0,
                baseline_wait_minutes=0,
                difference_minutes=0,
                estimation_source="ML_MODEL" if self.is_loaded else "BASELINE_FALLBACK",
                model_name=self.model_name if self.is_loaded else "Deterministic Baseline",
                factors_considered={
                    "people_ahead": 0,
                    "active_counters": req.active_counters,
                    "service_duration_min": req.service_duration_min,
                    "is_peak_hour": bool(is_peak_hour),
                    "is_weekend": bool(is_weekend)
                }
            )

        if self.is_loaded and self.pipeline is not None:
            try:
                input_df = pd.DataFrame([{
                    'people_ahead': req.people_ahead,
                    'active_counters': req.active_counters,
                    'service_duration_min': req.service_duration_min,
                    'hour_of_day': hour,
                    'day_of_week': day,
                    'is_weekend': is_weekend,
                    'is_peak_hour': is_peak_hour,
                    'baseline_estimate': baseline,
                    'service_category': category
                }])

                raw_prediction = self.pipeline.predict(input_df)[0]
                predicted_wait = max(1, int(round(raw_prediction)))
                source = "ML_MODEL"
                model_used = self.model_name
            except Exception as e:
                logger.error(f"Inference error: {e}. Falling back to baseline formula.", exc_info=True)
                predicted_wait = baseline
                source = "BASELINE_FALLBACK"
                model_used = "Deterministic Baseline"
        else:
            predicted_wait = baseline
            source = "BASELINE_FALLBACK"
            model_used = "Deterministic Baseline"

        diff = predicted_wait - baseline

        return PredictResponse(
            predicted_wait_minutes=predicted_wait,
            baseline_wait_minutes=baseline,
            difference_minutes=diff,
            estimation_source=source,
            model_name=model_used,
            factors_considered={
                "people_ahead": req.people_ahead,
                "active_counters": req.active_counters,
                "service_duration_min": req.service_duration_min,
                "hour_of_day": hour,
                "day_of_week": day,
                "is_peak_hour": bool(is_peak_hour),
                "is_weekend": bool(is_weekend),
                "service_category": category,
                "rush_adjustment_minutes": diff
            }
        )
