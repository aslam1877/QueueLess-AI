"""
Queue dataset generator for wait time modeling.
Generates queue records capturing rush periods, counter counts, and service durations.
"""

import os
import math
import numpy as np
import pandas as pd

def generate_synthetic_queue_data(num_samples: int = 10000, seed: int = 42) -> pd.DataFrame:
    np.random.seed(seed)

    people_ahead_raw = np.random.negative_binomial(n=4, p=0.4, size=num_samples)
    people_ahead = np.clip(people_ahead_raw, 0, 30)

    counter_choices = [1, 2, 3, 4]
    counter_probs = [0.35, 0.40, 0.15, 0.10]
    active_counters = np.random.choice(counter_choices, size=num_samples, p=counter_probs)

    service_durations = np.random.choice(
        [10.0, 15.0, 20.0, 30.0, 45.0, 60.0],
        size=num_samples,
        p=[0.10, 0.25, 0.30, 0.20, 0.10, 0.05]
    )
    service_duration_min = np.round(service_durations * np.random.uniform(0.9, 1.1, size=num_samples), 1)

    hour_of_day = np.random.choice(
        np.arange(8, 20),
        size=num_samples,
        p=[0.05, 0.08, 0.10, 0.12, 0.15, 0.12, 0.08, 0.08, 0.09, 0.07, 0.04, 0.02]
    )

    day_of_week = np.random.choice(
        np.arange(7),
        size=num_samples,
        p=[0.14, 0.14, 0.14, 0.15, 0.18, 0.17, 0.08]
    )

    categories = ['CONSULTATION', 'ROUTINE_CHECKUP', 'EXPRESS_SERVICE', 'REPAIR_DIAGNOSTIC', 'PROCEDURE']
    category_choices = np.random.choice(categories, size=num_samples)

    is_weekend = np.isin(day_of_week, [5, 6]).astype(int)
    is_peak_hour = np.isin(hour_of_day, [11, 12, 13, 17, 18]).astype(int)

    baseline_estimate = np.ceil((people_ahead * service_duration_min) / active_counters).astype(int)

    actual_wait_minutes = np.zeros(num_samples, dtype=float)

    for i in range(num_samples):
        pa = people_ahead[i]
        c = active_counters[i]
        dur = service_duration_min[i]
        base = baseline_estimate[i]

        if pa == 0:
            # When you are next in line, wait time is near 0 or just a couple minutes if counter finishing up
            actual_wait = max(0, round(np.random.exponential(scale=1.2)))
        else:
            # Non-linear congestion multiplier:
            multiplier = 1.0

            # Peak hour slowdown: staff takes breaks, customers arrive with complex needs
            if is_peak_hour[i]:
                multiplier += 0.22

            # Weekend rush: higher variance and distraction
            if is_weekend[i]:
                multiplier += 0.14

            # Queue backlog overhead: when queue > 8 people, per-customer latency increases
            if pa > 8:
                multiplier += 0.015 * (pa - 8)

            # End of day fatigue (hour 17+)
            if hour_of_day[i] >= 17:
                multiplier += 0.08

            # Category-specific variances
            if category_choices[i] == 'PROCEDURE':
                multiplier += 0.12  # Procedures often run over
            elif category_choices[i] == 'EXPRESS_SERVICE':
                multiplier -= 0.10  # Express moves faster

            # Realistic noise: unpredicted customer delays, paperwork delays
            noise = np.random.normal(loc=0.0, scale=max(2.0, base * 0.08))

            actual_wait = max(1.0, round(base * multiplier + noise))

        actual_wait_minutes[i] = int(actual_wait)

    df = pd.DataFrame({
        'people_ahead': people_ahead,
        'active_counters': active_counters,
        'service_duration_min': service_duration_min,
        'hour_of_day': hour_of_day,
        'day_of_week': day_of_week,
        'is_weekend': is_weekend,
        'is_peak_hour': is_peak_hour,
        'service_category': category_choices,
        'baseline_estimate': baseline_estimate,
        'actual_wait_minutes': actual_wait_minutes.astype(int)
    })

    return df

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(os.path.dirname(script_dir), 'data')
    os.makedirs(data_dir, exist_ok=True)

    output_path = os.path.join(data_dir, 'queue_training_data.csv')
    print("Generating 10,000 synthetic queue records...")
    df = generate_synthetic_queue_data(num_samples=10000, seed=42)
    df.to_csv(output_path, index=False)

    print(f"Dataset successfully saved to: {output_path}")
    print(f"Shape: {df.shape}")
    print("\nDataset Summary Statistics:")
    print(df[['people_ahead', 'active_counters', 'service_duration_min', 'baseline_estimate', 'actual_wait_minutes']].describe().round(2))
    print("\nSample rows:")
    print(df.head(5))

if __name__ == '__main__':
    main()
