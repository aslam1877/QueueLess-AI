package com.queueless.dto.response;

import java.time.LocalDateTime;

/**
 * Detailed response for wait time estimation.
 * Provides the calculated wait time, the formula/source used,
 * and contextual queue factors that influenced the calculation.
 *
 * Used by: GET /api/queue-entries/{id}/estimate
 */
public class WaitTimeEstimateResponse {

    private Long queueEntryId;
    private String tokenNumber;
    private String status;
    private int position;
    private int peopleAhead;
    private int activeCounters;
    private int serviceDurationMinutes;
    private int estimatedWaitMinutes;
    private String estimationSource;
    private LocalDateTime estimatedServiceTime;
    private LocalDateTime calculatedAt;
    private String explanation;

    public WaitTimeEstimateResponse() {
        this.calculatedAt = LocalDateTime.now();
    }

    public WaitTimeEstimateResponse(Long queueEntryId,
                                    String tokenNumber,
                                    String status,
                                    int position,
                                    int peopleAhead,
                                    int activeCounters,
                                    int serviceDurationMinutes,
                                    int estimatedWaitMinutes,
                                    String estimationSource,
                                    LocalDateTime estimatedServiceTime,
                                    String explanation) {
        this.queueEntryId = queueEntryId;
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.position = position;
        this.peopleAhead = peopleAhead;
        this.activeCounters = activeCounters;
        this.serviceDurationMinutes = serviceDurationMinutes;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.estimationSource = estimationSource;
        this.estimatedServiceTime = estimatedServiceTime;
        this.calculatedAt = LocalDateTime.now();
        this.explanation = explanation;
    }

    // Getters and Setters
    public Long getQueueEntryId() { return queueEntryId; }
    public void setQueueEntryId(Long queueEntryId) { this.queueEntryId = queueEntryId; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getPeopleAhead() { return peopleAhead; }
    public void setPeopleAhead(int peopleAhead) { this.peopleAhead = peopleAhead; }

    public int getActiveCounters() { return activeCounters; }
    public void setActiveCounters(int activeCounters) { this.activeCounters = activeCounters; }

    public int getServiceDurationMinutes() { return serviceDurationMinutes; }
    public void setServiceDurationMinutes(int serviceDurationMinutes) { this.serviceDurationMinutes = serviceDurationMinutes; }

    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }

    public String getEstimationSource() { return estimationSource; }
    public void setEstimationSource(String estimationSource) { this.estimationSource = estimationSource; }

    public LocalDateTime getEstimatedServiceTime() { return estimatedServiceTime; }
    public void setEstimatedServiceTime(LocalDateTime estimatedServiceTime) { this.estimatedServiceTime = estimatedServiceTime; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
