package com.queueless.dto.request;

import jakarta.validation.constraints.Min;


public class OpenQueueRequest {

    @Min(value = 1, message = "Must have at least 1 active counter")
    private Integer activeCounters = 1;

    public OpenQueueRequest() {}

    public OpenQueueRequest(Integer activeCounters) {
        this.activeCounters = activeCounters;
    }

    public Integer getActiveCounters() { return activeCounters; }
    public void setActiveCounters(Integer activeCounters) { this.activeCounters = activeCounters; }
}
