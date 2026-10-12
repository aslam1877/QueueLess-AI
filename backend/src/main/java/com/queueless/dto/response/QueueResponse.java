package com.queueless.dto.response;

import com.queueless.entity.Queue;
import com.queueless.enums.QueueStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class QueueResponse {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private String businessName;
    private QueueStatus status;
    private LocalDate queueDate;
    private Integer currentTokenNumber;
    private Integer activeCounters;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public QueueResponse() {}

    public static QueueResponse from(Queue queue) {
        QueueResponse r = new QueueResponse();
        r.id = queue.getId();
        r.serviceId = queue.getService().getId();
        r.serviceName = queue.getService().getName();
        r.businessName = queue.getService().getBusiness().getName();
        r.status = queue.getStatus();
        r.queueDate = queue.getQueueDate();
        r.currentTokenNumber = queue.getCurrentTokenNumber();
        r.activeCounters = queue.getActiveCounters();
        r.createdAt = queue.getCreatedAt();
        r.closedAt = queue.getClosedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public String getBusinessName() { return businessName; }
    public QueueStatus getStatus() { return status; }
    public LocalDate getQueueDate() { return queueDate; }
    public Integer getCurrentTokenNumber() { return currentTokenNumber; }
    public Integer getActiveCounters() { return activeCounters; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
