from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .schemas import (
    PredictRequest,
    PredictResponse,
    HealthResponse,
    ModelInfoResponse
)
from .model import WaitTimePredictor

app = FastAPI(
    title="Wait Time Prediction Service",
    version="1.0.0"
)

# Enable CORS for local cross-service communication
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Instantiate prediction engine
predictor = WaitTimePredictor()

@app.get("/", tags=["General"])
def root():
    return {
        "service": "QueueLess AI - ML Prediction Microservice",
        "version": "1.0.0",
        "status": "UP",
        "docs_url": "/docs",
        "health_url": "/health"
    }

@app.get("/health", response_model=HealthResponse, tags=["Health"])
def health_check():
    """Health check endpoint called by Spring Boot and orchestrators."""
    return HealthResponse(
        status="UP",
        model_loaded=predictor.is_loaded,
        model_name=predictor.model_name,
        timestamp=datetime.now().isoformat()
    )

@app.post("/predict", response_model=PredictResponse, tags=["Prediction"])
def predict_wait_time(request: PredictRequest):
    """
    Accepts current queue conditions and predicts actual wait time in minutes.
    Falls back gracefully to deterministic math if ML pipeline has an issue.
    """
    return predictor.predict(request)

@app.get("/model-info", response_model=ModelInfoResponse, tags=["Model Info"])
def get_model_info():
    """Returns training metrics, dataset summary, and feature importances."""
    metrics = predictor.metrics
    return ModelInfoResponse(
        model_name=predictor.model_name,
        trained_at=metrics.get("trained_at"),
        dataset_samples=metrics.get("dataset_samples"),
        baseline_benchmark=metrics.get("baseline_benchmark"),
        ml_metrics=metrics.get("ml_metrics"),
        top_feature_importances=metrics.get("top_feature_importances")
    )
