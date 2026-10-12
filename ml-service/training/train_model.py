"""
Model training script for wait time prediction.
Trains candidate regressors and saves the best performing model.
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
from datetime import datetime

from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.linear_model import Ridge
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

def train_and_evaluate():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    data_path = os.path.join(project_root, 'data', 'queue_training_data.csv')
    models_dir = os.path.join(project_root, 'models')
    os.makedirs(models_dir, exist_ok=True)

    print(f"Loading training data from {data_path}...")
    df = pd.read_csv(data_path)
    print(f"Loaded {len(df)} records.")

    y = df['actual_wait_minutes'].values

    numerical_features = [
        'people_ahead',
        'active_counters',
        'service_duration_min',
        'hour_of_day',
        'day_of_week',
        'is_weekend',
        'is_peak_hour',
        'baseline_estimate'
    ]
    categorical_features = ['service_category']

    feature_cols = numerical_features + categorical_features
    X = df[feature_cols]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42
    )
    print(f"Train size: {len(X_train)}, Test size: {len(X_test)}")

    baseline_preds = X_test['baseline_estimate'].values
    baseline_mae = mean_absolute_error(y_test, baseline_preds)
    baseline_rmse = np.sqrt(mean_squared_error(y_test, baseline_preds))
    baseline_r2 = r2_score(y_test, baseline_preds)

    print("\n" + "=" * 60)
    print("  BENCHMARK: DETERMINISTIC BASELINE FORMULA")
    print("=" * 60)
    print(f"  MAE : {baseline_mae:.2f} minutes")
    print(f"  RMSE: {baseline_rmse:.2f} minutes")
    print(f"  R²  : {baseline_r2:.4f}")

    # Build preprocessing pipeline
    preprocessor = ColumnTransformer(
        transformers=[
            ('num', StandardScaler(), numerical_features),
            ('cat', OneHotEncoder(handle_unknown='ignore', sparse_output=False), categorical_features)
        ]
    )

    candidate_models = {
        'Ridge Regression': Ridge(alpha=1.0),
        'Random Forest': RandomForestRegressor(n_estimators=100, max_depth=12, random_state=42, n_jobs=-1),
        'Gradient Boosting': GradientBoostingRegressor(n_estimators=120, learning_rate=0.08, max_depth=5, random_state=42)
    }

    results = {}
    best_model_name = None
    best_mae = float('inf')
    best_pipeline = None

    print("\n" + "=" * 60)
    print("  EVALUATING MACHINE LEARNING REGRESSION MODELS")
    print("=" * 60)

    for name, regressor in candidate_models.items():
        print(f"\nTraining {name}...")
        pipeline = Pipeline([
            ('preprocessor', preprocessor),
            ('regressor', regressor)
        ])

        pipeline.fit(X_train, y_train)
        preds = pipeline.predict(X_test)
        # Prediction post-processing: wait time cannot be negative
        preds = np.clip(preds, 0, None)

        mae = mean_absolute_error(y_test, preds)
        rmse = np.sqrt(mean_squared_error(y_test, preds))
        r2 = r2_score(y_test, preds)
        improvement = ((baseline_mae - mae) / baseline_mae) * 100

        print(f"  MAE : {mae:.2f} minutes (Improvement over baseline: +{improvement:.1f}%)")
        print(f"  RMSE: {rmse:.2f} minutes")
        print(f"  R²  : {r2:.4f}")

        results[name] = {
            'mae': round(float(mae), 2),
            'rmse': round(float(rmse), 2),
            'r2': round(float(r2), 4),
            'mae_improvement_pct': round(float(improvement), 1)
        }

        if mae < best_mae:
            best_mae = mae
            best_model_name = name
            best_pipeline = pipeline

    print("\n" + "=" * 60)
    print(f"  WINNING MODEL: {best_model_name} (MAE: {best_mae:.2f} mins)")
    print("=" * 60)

    # Serialize best pipeline
    model_save_path = os.path.join(models_dir, 'wait_time_model.joblib')
    joblib.dump(best_pipeline, model_save_path)
    print(f"Winning model saved to: {model_save_path}")

    # Extract feature importances if available
    regressor = best_pipeline.named_steps['regressor']
    feature_importances = {}
    if hasattr(regressor, 'feature_importances_'):
        # Get one-hot feature names
        cat_encoder = best_pipeline.named_steps['preprocessor'].named_transformers_['cat']
        encoded_cat_features = cat_encoder.get_feature_names_out(categorical_features).tolist()
        all_features = numerical_features + encoded_cat_features
        importances = regressor.feature_importances_

        sorted_indices = np.argsort(importances)[::-1]
        for idx in sorted_indices[:10]:
            feature_importances[all_features[idx]] = round(float(importances[idx]), 4)

    # Save metadata JSON
    metrics_metadata = {
        'model_name': best_model_name,
        'trained_at': datetime.now().isoformat(),
        'dataset_samples': len(df),
        'test_split_ratio': 0.20,
        'features_used': feature_cols,
        'baseline_benchmark': {
            'mae': round(float(baseline_mae), 2),
            'rmse': round(float(baseline_rmse), 2),
            'r2': round(float(baseline_r2), 4)
        },
        'ml_metrics': results[best_model_name],
        'candidate_comparisons': results,
        'top_feature_importances': feature_importances
    }

    metrics_save_path = os.path.join(models_dir, 'model_metrics.json')
    with open(metrics_save_path, 'w') as f:
        json.dump(metrics_metadata, f, indent=2)
    print(f"Model metrics metadata saved to: {metrics_save_path}")

    print("\nTop Feature Importances:")
    for feat, imp in feature_importances.items():
        print(f"  - {feat}: {imp * 100:.2f}%")

if __name__ == '__main__':
    train_and_evaluate()
