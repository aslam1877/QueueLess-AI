package com.queueless.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO sent to the Python FastAPI microservice (POST /predict).
 * Uses Jackson @JsonProperty mappings to match Python's snake_case schema.
 */
public class PredictionRequest {

    @JsonProperty("people_ahead")
    private Integer peopleAhead;

    @JsonProperty("active_counters")
    private Integer activeCounters;

    @JsonProperty("service_duration_min")
    private Double serviceDurationMin;

    @JsonProperty("hour_of_day")
    private Integer hourOfDay;

    @JsonProperty("day_of_week")
    private Integer dayOfWeek;

    @JsonProperty("service_category")
    private String serviceCategory;

    public PredictionRequest() {}

    public PredictionRequest(Integer peopleAhead,
                             Integer activeCounters,
                             Double serviceDurationMin,
                             Integer hourOfDay,
                             Integer dayOfWeek,
                             String serviceCategory) {
        this.peopleAhead = peopleAhead;
        this.activeCounters = activeCounters;
        this.serviceDurationMin = serviceDurationMin;
        this.hourOfDay = hourOfDay;
        this.dayOfWeek = dayOfWeek;
        this.serviceCategory = serviceCategory;
    }

    public Integer getPeopleAhead() { return peopleAhead; }
    public void setPeopleAhead(Integer peopleAhead) { this.peopleAhead = peopleAhead; }

    public Integer getActiveCounters() { return activeCounters; }
    public void setActiveCounters(Integer activeCounters) { this.activeCounters = activeCounters; }

    public Double getServiceDurationMin() { return serviceDurationMin; }
    public void setServiceDurationMin(Double serviceDurationMin) { this.serviceDurationMin = serviceDurationMin; }

    public Integer getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }
}
