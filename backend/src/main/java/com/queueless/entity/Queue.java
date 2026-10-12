package com.queueless.entity;

import com.queueless.enums.QueueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "queues")
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QueueStatus status;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "current_token_number", nullable = false)
    private Integer currentTokenNumber = 0;

    @Column(name = "active_counters", nullable = false)
    private Integer activeCounters = 1;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    protected Queue() {
    }

    public Queue(ServiceEntity service, LocalDate queueDate, Integer activeCounters) {
        this.service = service;
        this.status = QueueStatus.OPEN;
        this.queueDate = queueDate;
        this.currentTokenNumber = 0;
        this.activeCounters = activeCounters != null ? activeCounters : 1;
        this.createdAt = LocalDateTime.now();
    }

    public int nextTokenNumber() {
        this.currentTokenNumber++;
        return this.currentTokenNumber;
    }

    public void close() {
        this.status = QueueStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return this.status == QueueStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public ServiceEntity getService() {
        return service;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public Integer getCurrentTokenNumber() {
        return currentTokenNumber;
    }

    public Integer getActiveCounters() {
        return activeCounters;
    }

    public void setActiveCounters(Integer activeCounters) {
        this.activeCounters = activeCounters;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    @Override
    public String toString() {
        return "Queue{" +
                "id=" + id +
                ", status=" + status +
                ", queueDate=" + queueDate +
                ", currentTokenNumber=" + currentTokenNumber +
                ", activeCounters=" + activeCounters +
                '}';
    }
}
