"""
QueueLess AI - ML Microservice Automated Test Suite
Tests FastAPI endpoints: /health, /model-info, /predict, and fallback behaviors.
"""

import pytest
from fastapi.testclient import TestClient
from app.main import app, predictor

client = TestClient(app)

def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "version" in data

def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["model_loaded"] is True
    assert "model_name" in data

def test_model_info_endpoint():
    response = client.get("/model-info")
    assert response.status_code == 200
    data = response.json()
    assert data["model_name"] != ""
    assert data["ml_metrics"] is not None
    assert "mae" in data["ml_metrics"]
    assert "r2" in data["ml_metrics"]
    assert data["top_feature_importances"] is not None

def test_predict_standard_case():
    payload = {
        "people_ahead": 4,
        "active_counters": 2,
        "service_duration_min": 20.0,
        "hour_of_day": 12,  # Lunch peak
        "day_of_week": 4,   # Friday
        "service_category": "CONSULTATION"
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()

    assert data["baseline_wait_minutes"] == 40  # ceil(4 * 20 / 2) = 40
    assert data["predicted_wait_minutes"] > 0
    assert data["estimation_source"] == "ML_MODEL"
    assert "rush_adjustment_minutes" in data["factors_considered"]

def test_predict_zero_people_ahead():
    payload = {
        "people_ahead": 0,
        "active_counters": 2,
        "service_duration_min": 30.0
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["predicted_wait_minutes"] == 0
    assert data["baseline_wait_minutes"] == 0

def test_predict_validation_errors():
    # Negative people ahead
    res1 = client.post("/predict", json={"people_ahead": -2, "service_duration_min": 15})
    assert res1.status_code == 422

    # Zero active counters
    res2 = client.post("/predict", json={"people_ahead": 3, "active_counters": 0, "service_duration_min": 15})
    assert res2.status_code == 422

    # Non-positive duration
    res3 = client.post("/predict", json={"people_ahead": 3, "service_duration_min": 0})
    assert res3.status_code == 422

def test_predict_fallback_resilience():
    # Temporarily simulate unloaded model
    original_loaded = predictor.is_loaded
    original_pipeline = predictor.pipeline
    try:
        predictor.is_loaded = False
        predictor.pipeline = None

        payload = {
            "people_ahead": 5,
            "active_counters": 2,
            "service_duration_min": 20.0
        }
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["estimation_source"] == "BASELINE_FALLBACK"
        # Deterministic baseline: ceil(5 * 20 / 2) = 50 mins
        assert data["predicted_wait_minutes"] == 50
        assert data["baseline_wait_minutes"] == 50
    finally:
        predictor.is_loaded = original_loaded
        predictor.pipeline = original_pipeline
