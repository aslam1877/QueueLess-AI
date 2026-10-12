"""
QueueLess AI - Model Evaluation & Benchmarking Script
Provides detailed granular diagnostics:
- Error breakdown by queue length (short: 0-3, medium: 4-8, long: 9+)
- Error breakdown by time-of-day (peak vs non-peak)
- Direct comparison table against deterministic baseline
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error, r2_score

def evaluate():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    data_path = os.path.join(project_root, 'data', 'queue_training_data.csv')
    model_path = os.path.join(project_root, 'models', 'wait_time_model.joblib')
    metrics_path = os.path.join(project_root, 'models', 'model_metrics.json')

    print(f"Loading model from {model_path}...")
    pipeline = joblib.load(model_path)
    df = pd.read_csv(data_path)

    # Use held-out test split for evaluation
    test_df = df.sample(frac=0.2, random_state=42)
    y_true = test_df['actual_wait_minutes'].values
    y_baseline = test_df['baseline_estimate'].values

    features = [
        'people_ahead', 'active_counters', 'service_duration_min',
        'hour_of_day', 'day_of_week', 'is_weekend', 'is_peak_hour',
        'baseline_estimate', 'service_category'
    ]
    y_pred = np.clip(pipeline.predict(test_df[features]), 0, None)

    print("\n" + "=" * 65)
    print("      OVERALL MODEL BENCHMARK vs DETERMINISTIC BASELINE")
    print("=" * 65)
    base_mae = mean_absolute_error(y_true, y_baseline)
    ml_mae = mean_absolute_error(y_true, y_pred)
    improvement = ((base_mae - ml_mae) / base_mae) * 100

    print(f"  Baseline MAE : {base_mae:.2f} mins")
    print(f"  ML Model MAE : {ml_mae:.2f} mins")
    print(f"  Error Reduced: {improvement:.1f}%")
    print(f"  ML Model R²  : {r2_score(y_true, y_pred):.4f}")

    # Sub-cohort: Peak vs Off-Peak
    print("\n" + "-" * 65)
    print("  PERFORMANCE BREAKDOWN: PEAK HOURS vs OFF-PEAK")
    print("-" * 65)
    for is_peak, label in [(1, "Peak Hours (11-13, 17-18)"), (0, "Off-Peak Hours")]:
        mask = (test_df['is_peak_hour'] == is_peak)
        sub_true = y_true[mask]
        sub_base = y_baseline[mask]
        sub_pred = y_pred[mask]
        sub_base_mae = mean_absolute_error(sub_true, sub_base)
        sub_ml_mae = mean_absolute_error(sub_true, sub_pred)
        sub_imp = ((sub_base_mae - sub_ml_mae) / sub_base_mae) * 100
        print(f"  [{label}] (n={mask.sum()}):")
        print(f"     Baseline MAE: {sub_base_mae:.2f}m | ML MAE: {sub_ml_mae:.2f}m | Improvement: +{sub_imp:.1f}%")

    # Sub-cohort: Queue Length
    print("\n" + "-" * 65)
    print("  PERFORMANCE BREAKDOWN: QUEUE LENGTH COHORTS")
    print("-" * 65)
    cohorts = [
        ("Short Queue (0-3 ahead)", test_df['people_ahead'] <= 3),
        ("Medium Queue (4-8 ahead)", (test_df['people_ahead'] > 3) & (test_df['people_ahead'] <= 8)),
        ("Long Queue (9+ ahead)", test_df['people_ahead'] > 8)
    ]
    for label, mask in cohorts:
        sub_true = y_true[mask]
        sub_base = y_baseline[mask]
        sub_pred = y_pred[mask]
        sub_base_mae = mean_absolute_error(sub_true, sub_base)
        sub_ml_mae = mean_absolute_error(sub_true, sub_pred)
        sub_imp = ((sub_base_mae - sub_ml_mae) / sub_base_mae) * 100
        print(f"  [{label}] (n={mask.sum()}):")
        print(f"     Baseline MAE: {sub_base_mae:.2f}m | ML MAE: {sub_ml_mae:.2f}m | Improvement: +{sub_imp:.1f}%")

    print("\n" + "=" * 65)
    print("  Evaluation successfully completed.")

if __name__ == '__main__':
    evaluate()
