package com.queueless.entity;

import com.queueless.enums.EntryStatus;
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
import java.time.LocalDateTime;

/**
 * Queue entry entity tracking customer lifecycle and servicing timestamps.
 */
@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private Queue queue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** The token displayed to the customer, e.g., "T-001" */
    @Column(name = "token_number", nullable = false, length = 20)
    private String tokenNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EntryStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime calledAt;

    private LocalDateTime serviceStartedAt;

    private LocalDateTime serviceCompletedAt;

    /** Estimated wait in minutes (from baseline or ML model). */
    @Column(name = "estimated_wait_minutes")
    private Integer estimatedWaitMinutes;

    /** Where the estimate came from: "BASELINE" or "ML_MODEL". */
    @Column(name = "estimation_source", length = 20)
    private String estimationSource;

    protected QueueEntry() {
    }

    public QueueEntry(Queue queue, User customer, String tokenNumber) {
        this.queue = queue;
        this.customer = customer;
        this.tokenNumber = tokenNumber;
        this.status = EntryStatus.WAITING;
        this.joinedAt = LocalDateTime.now();
    }

    /**
     * Mark this entry as CALLED (staff announces the token).
     *
     * @throws IllegalStateException if current status is not WAITING
     */
    public void markCalled() {
        validateTransition(EntryStatus.WAITING, EntryStatus.CALLED);
        this.status = EntryStatus.CALLED;
        this.calledAt = LocalDateTime.now();
    }

    /**
     * Mark service as started.
     *
     * @throws IllegalStateException if current status is not CALLED
     */
    public void markInService() {
        validateTransition(EntryStatus.CALLED, EntryStatus.IN_SERVICE);
        this.status = EntryStatus.IN_SERVICE;
        this.serviceStartedAt = LocalDateTime.now();
    }

    /**
     * Mark service as completed.
     *
     * @throws IllegalStateException if current status is not IN_SERVICE
     */
    public void markCompleted() {
        validateTransition(EntryStatus.IN_SERVICE, EntryStatus.COMPLETED);
        this.status = EntryStatus.COMPLETED;
        this.serviceCompletedAt = LocalDateTime.now();
    }

    /**
     * Skip this entry (customer didn't show up).
     * Can be skipped from WAITING or CALLED.
     *
     * @throws IllegalStateException if current status doesn't allow skipping
     */
    public void markSkipped() {
        if (this.status != EntryStatus.WAITING && this.status != EntryStatus.CALLED) {
            throw new IllegalStateException(
                    "Cannot skip entry with status " + this.status +
                            ". Entry must be WAITING or CALLED to be skipped.");
        }
        this.status = EntryStatus.SKIPPED;
    }

    /**
     * Cancel this entry (customer voluntarily leaves).
     * Can be cancelled from WAITING or CALLED.
     *
     * @throws IllegalStateException if current status doesn't allow cancellation
     */
    public void markCancelled() {
        if (this.status != EntryStatus.WAITING && this.status != EntryStatus.CALLED) {
            throw new IllegalStateException(
                    "Cannot cancel entry with status " + this.status +
                            ". Entry must be WAITING or CALLED to be cancelled.");
        }
        this.status = EntryStatus.CANCELLED;
    }

    /**
     * Validates that a status transition is legal.
     * Throws IllegalStateException with a clear message if not.
     */
    private void validateTransition(EntryStatus expectedCurrent, EntryStatus target) {
        if (this.status != expectedCurrent) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + target +
                            ". Expected current status: " + expectedCurrent);
        }
    }

    /**
     * Whether this entry is in a terminal state (can't change further).
     */
    public boolean isTerminal() {
        return this.status == EntryStatus.COMPLETED ||
                this.status == EntryStatus.SKIPPED ||
                this.status == EntryStatus.CANCELLED;
    }

    /**
     * Whether this entry is still active (waiting or being served).
     */
    public boolean isActive() {
        return this.status == EntryStatus.WAITING ||
                this.status == EntryStatus.CALLED ||
                this.status == EntryStatus.IN_SERVICE;
    }

    public Long getId() {
        return id;
    }

    public Queue getQueue() {
        return queue;
    }

    public User getCustomer() {
        return customer;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public EntryStatus getStatus() {
        return status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public LocalDateTime getServiceStartedAt() {
        return serviceStartedAt;
    }

    public LocalDateTime getServiceCompletedAt() {
        return serviceCompletedAt;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public String getEstimationSource() {
        return estimationSource;
    }

    public void setEstimationSource(String estimationSource) {
        this.estimationSource = estimationSource;
    }

    @Override
    public String toString() {
        return "QueueEntry{" +
                "id=" + id +
                ", tokenNumber='" + tokenNumber + '\'' +
                ", status=" + status +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
