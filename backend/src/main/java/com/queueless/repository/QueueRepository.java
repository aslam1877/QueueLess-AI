package com.queueless.repository;

import com.queueless.entity.Queue;
import com.queueless.enums.QueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository interface for Queue persistence and row-level locking operations.
 */
public interface QueueRepository extends JpaRepository<Queue, Long> {

    /**
     * Find a queue by ID and lock the row for update.
     * Must be called within a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Queue q WHERE q.id = :id")
    Optional<Queue> findByIdForUpdate(@Param("id") Long id);

    /** Find today's queue for a specific service. */
    Optional<Queue> findByServiceIdAndQueueDate(Long serviceId, LocalDate queueDate);

    /** Find the open queue for a service on a specific date. */
    Optional<Queue> findByServiceIdAndQueueDateAndStatus(
            Long serviceId, LocalDate queueDate, QueueStatus status);
}
