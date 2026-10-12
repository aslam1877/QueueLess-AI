package com.queueless.dto.response;

import com.queueless.entity.QueueEntry;
import com.queueless.enums.EntryStatus;
import java.time.LocalDateTime;

/**
 * Response DTO for a customer's queue entry.
 * Shows token, status, timestamps — but NOT customer details
 * in public queue displays (privacy).
 */
public class QueueEntryResponse {

    private Long id;
    private Long queueId;
    private Long customerId;
    private String customerName;
    private String tokenNumber;
    private EntryStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime calledAt;
    private LocalDateTime serviceStartedAt;
    private LocalDateTime serviceCompletedAt;
    private Integer estimatedWaitMinutes;
    private String estimationSource;

    public QueueEntryResponse() {}

    public static QueueEntryResponse from(QueueEntry entry) {
        QueueEntryResponse r = new QueueEntryResponse();
        r.id = entry.getId();
        r.queueId = entry.getQueue().getId();
        r.customerId = entry.getCustomer().getId();
        r.customerName = entry.getCustomer().getName();
        r.tokenNumber = entry.getTokenNumber();
        r.status = entry.getStatus();
        r.joinedAt = entry.getJoinedAt();
        r.calledAt = entry.getCalledAt();
        r.serviceStartedAt = entry.getServiceStartedAt();
        r.serviceCompletedAt = entry.getServiceCompletedAt();
        r.estimatedWaitMinutes = entry.getEstimatedWaitMinutes();
        r.estimationSource = entry.getEstimationSource();
        return r;
    }

    public Long getId() { return id; }
    public Long getQueueId() { return queueId; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getTokenNumber() { return tokenNumber; }
    public EntryStatus getStatus() { return status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getCalledAt() { return calledAt; }
    public LocalDateTime getServiceStartedAt() { return serviceStartedAt; }
    public LocalDateTime getServiceCompletedAt() { return serviceCompletedAt; }
    public Integer getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public String getEstimationSource() { return estimationSource; }
}
