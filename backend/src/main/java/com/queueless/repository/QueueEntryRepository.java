package com.queueless.repository;

import com.queueless.entity.QueueEntry;
import com.queueless.enums.EntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    /** Find all entries in a queue, ordered by join time (FIFO). */
    List<QueueEntry> findByQueueIdOrderByJoinedAtAsc(Long queueId);

    /** Find entries in a queue with a specific status. */
    List<QueueEntry> findByQueueIdAndStatusOrderByJoinedAtAsc(Long queueId, EntryStatus status);

    /** Check if a customer already has an active entry in this queue. */
    boolean existsByQueueIdAndCustomerIdAndStatusIn(
            Long queueId, Long customerId, List<EntryStatus> statuses);

    /** Find a customer's active entry in a queue. */
    Optional<QueueEntry> findByQueueIdAndCustomerIdAndStatusIn(
            Long queueId, Long customerId, List<EntryStatus> statuses);

    /** Count waiting customers ahead of a specific entry (by join time). */
    @Query("SELECT COUNT(e) FROM QueueEntry e " +
           "WHERE e.queue.id = :queueId " +
           "AND e.status = 'WAITING' " +
           "AND e.joinedAt < :joinedAt")
    long countWaitingAhead(@Param("queueId") Long queueId,
                           @Param("joinedAt") java.time.LocalDateTime joinedAt);

    /** Count total entries with a specific status in a queue. */
    long countByQueueIdAndStatus(Long queueId, EntryStatus status);

    /** Find the next WAITING entry (earliest joinedAt). */
    Optional<QueueEntry> findFirstByQueueIdAndStatusOrderByJoinedAtAsc(
            Long queueId, EntryStatus status);

    /** Find all entries for a customer across all queues. */
    List<QueueEntry> findByCustomerIdOrderByJoinedAtDesc(Long customerId);

    /**
     * Find recent completed entries for a specific service that have valid start and completion timestamps.
     * Used by WaitTimeEstimationService to compute empirical moving average service duration.
     */
    @Query("SELECT e FROM QueueEntry e " +
           "WHERE e.queue.service.id = :serviceId " +
           "AND e.status = 'COMPLETED' " +
           "AND e.serviceStartedAt IS NOT NULL " +
           "AND e.serviceCompletedAt IS NOT NULL " +
           "ORDER BY e.serviceCompletedAt DESC")
    List<QueueEntry> findRecentCompletedByServiceId(
            @Param("serviceId") Long serviceId,
            org.springframework.data.domain.Pageable pageable);
}
