from typing import Optional, Dict, Any
from pydantic import BaseModel, Field, ConfigDict

class PredictRequest(BaseModel):
    """Payload sent by Spring Boot (or client) to predict queue wait time."""
    people_ahead: int = Field(..., ge=0, description="Number of waiting customers ahead in queue")
    active_counters: int = Field(default=1, ge=1, description="Number of active service counters")
    service_duration_min: float = Field(..., gt=0, description="Configured or average service duration in minutes")
    hour_of_day: Optional[int] = Field(default=None, ge=0, le=23, description="Hour of day (0-23). Defaults to current hour.")
    day_of_week: Optional[int] = Field(default=None, ge=0, le=6, description="Day of week (0=Monday, 6=Sunday). Defaults to today.")
    service_category: Optional[str] = Field(default="ROUTINE_CHECKUP", description="Category or complexity of the service")

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "people_ahead": 5,
                "active_counters": 2,
                "service_duration_min": 25.0,
                "hour_of_day": 12,
                "day_of_week": 4,
                "service_category": "CONSULTATION"
            }
        }
    )

class PredictResponse(BaseModel):
    """Prediction result returned by the ML service."""
    predicted_wait_minutes: int
    baseline_wait_minutes: int
    difference_minutes: int
    estimation_source: str = Field(description="'ML_MODEL' or 'BASELINE_FALLBACK'")
    model_name: str
    factors_considered: Dict[str, Any]

class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    model_loaded: bool
    model_name: str
    timestamp: str

class ModelInfoResponse(BaseModel):
    """Detailed metadata about the trained model."""
    model_name: str
    trained_at: Optional[str] = None
    dataset_samples: Optional[int] = None
    baseline_benchmark: Optional[Dict[str, Any]] = None
    ml_metrics: Optional[Dict[str, Any]] = None
    top_feature_importances: Optional[Dict[str, float]] = None
