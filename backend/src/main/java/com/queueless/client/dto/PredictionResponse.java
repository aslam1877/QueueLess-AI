package com.queueless.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * DTO received from the Python FastAPI microservice (POST /predict).
 * Maps response fields from Python's snake_case schema.
 */
public class PredictionResponse {

    @JsonProperty("predicted_wait_minutes")
    private Integer predictedWaitMinutes;

    @JsonProperty("baseline_wait_minutes")
    private Integer baselineWaitMinutes;

    @JsonProperty("difference_minutes")
    private Integer differenceMinutes;

    @JsonProperty("estimation_source")
    private String estimationSource;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("factors_considered")
    private Map<String, Object> factorsConsidered;

    public PredictionResponse() {}

    public Integer getPredictedWaitMinutes() { return predictedWaitMinutes; }
    public void setPredictedWaitMinutes(Integer predictedWaitMinutes) { this.predictedWaitMinutes = predictedWaitMinutes; }

    public Integer getBaselineWaitMinutes() { return baselineWaitMinutes; }
    public void setBaselineWaitMinutes(Integer baselineWaitMinutes) { this.baselineWaitMinutes = baselineWaitMinutes; }

    public Integer getDifferenceMinutes() { return differenceMinutes; }
    public void setDifferenceMinutes(Integer differenceMinutes) { this.differenceMinutes = differenceMinutes; }

    public String getEstimationSource() { return estimationSource; }
    public void setEstimationSource(String estimationSource) { this.estimationSource = estimationSource; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Map<String, Object> getFactorsConsidered() { return factorsConsidered; }
    public void setFactorsConsidered(Map<String, Object> factorsConsidered) { this.factorsConsidered = factorsConsidered; }
}
