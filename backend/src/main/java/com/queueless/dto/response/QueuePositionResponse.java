package com.queueless.dto.response;

/**
 * What a customer sees about their queue position.
 * This is the main "customer dashboard" response.
 */
public class QueuePositionResponse {

    private String tokenNumber;
    private int position;
    private int peopleAhead;
    private String status;
    private Integer estimatedWaitMinutes;
    private Integer initialEstimatedWaitMinutes;
    private Integer currentEstimatedWaitMinutes;
    private String estimationSource;
    private String serviceName;
    private String businessName;
    private Long queueEntryId;

    public QueuePositionResponse() {}

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getPeopleAhead() { return peopleAhead; }
    public void setPeopleAhead(int peopleAhead) { this.peopleAhead = peopleAhead; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        if (this.currentEstimatedWaitMinutes == null) {
            this.currentEstimatedWaitMinutes = estimatedWaitMinutes;
        }
    }
    public Integer getInitialEstimatedWaitMinutes() { return initialEstimatedWaitMinutes; }
    public void setInitialEstimatedWaitMinutes(Integer initialEstimatedWaitMinutes) { this.initialEstimatedWaitMinutes = initialEstimatedWaitMinutes; }
    public Integer getCurrentEstimatedWaitMinutes() { return currentEstimatedWaitMinutes; }
    public void setCurrentEstimatedWaitMinutes(Integer currentEstimatedWaitMinutes) {
        this.currentEstimatedWaitMinutes = currentEstimatedWaitMinutes;
        this.estimatedWaitMinutes = currentEstimatedWaitMinutes;
    }
    public String getEstimationSource() { return estimationSource; }
    public void setEstimationSource(String estimationSource) { this.estimationSource = estimationSource; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public Long getQueueEntryId() { return queueEntryId; }
    public void setQueueEntryId(Long queueEntryId) { this.queueEntryId = queueEntryId; }
}
