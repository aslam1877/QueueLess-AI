package com.queueless.service;

import com.queueless.dto.request.OpenQueueRequest;
import com.queueless.dto.response.QueueEntryResponse;
import com.queueless.dto.response.QueuePositionResponse;
import com.queueless.dto.response.QueueResponse;
import com.queueless.dto.response.WaitTimeEstimateResponse;
import com.queueless.entity.Queue;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.ServiceEntity;
import com.queueless.entity.User;
import com.queueless.enums.EntryStatus;
import com.queueless.enums.QueueStatus;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.QueueEntryRepository;
import com.queueless.repository.QueueRepository;
import com.queueless.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing queue lifecycles, atomic token allocation, and state transitions.
 */
@Service
public class QueueService {

    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final ServiceManagementService serviceManagementService;
    private final UserRepository userRepository;
    private final WaitTimeEstimationService waitTimeEstimationService;

    // Active statuses that prevent a customer from joining again
    private static final List<EntryStatus> ACTIVE_STATUSES = List.of(
            EntryStatus.WAITING, EntryStatus.CALLED, EntryStatus.IN_SERVICE
    );

    public QueueService(QueueRepository queueRepository,
                        QueueEntryRepository queueEntryRepository,
                        ServiceManagementService serviceManagementService,
                        UserRepository userRepository,
                        WaitTimeEstimationService waitTimeEstimationService) {
        this.queueRepository = queueRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.serviceManagementService = serviceManagementService;
        this.userRepository = userRepository;
        this.waitTimeEstimationService = waitTimeEstimationService;
    }

    @Transactional
    public QueueResponse openQueue(Long serviceId, OpenQueueRequest request) {
        ServiceEntity service = serviceManagementService.getServiceEntityById(serviceId);

        LocalDate today = LocalDate.now();
        queueRepository.findByServiceIdAndQueueDateAndStatus(
                serviceId, today, QueueStatus.OPEN
        ).ifPresent(q -> {
            throw new DuplicateResourceException(
                    "An open queue already exists for this service today (Queue ID: " + q.getId() + ")");
        });

        Queue queue = new Queue(service, today, request.getActiveCounters());
        Queue saved = queueRepository.save(queue);
        return QueueResponse.from(saved);
    }

    @Transactional
    public QueueResponse closeQueue(Long queueId) {
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue not found with id: " + queueId));

        if (!queue.isOpen()) {
            throw new IllegalStateException("Queue is already closed");
        }

        queue.close();
        Queue saved = queueRepository.save(queue);
        return QueueResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public QueueResponse getQueueById(Long queueId) {
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue not found with id: " + queueId));
        return QueueResponse.from(queue);
    }

    @Transactional
    public QueueEntryResponse joinQueue(Long queueId, Long customerId) {
        Queue queue = queueRepository.findByIdForUpdate(queueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue not found with id: " + queueId));

        if (!queue.isOpen()) {
            throw new IllegalStateException("Queue is closed. Cannot join.");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + customerId));

        boolean alreadyInQueue = queueEntryRepository
                .existsByQueueIdAndCustomerIdAndStatusIn(
                        queueId, customerId, ACTIVE_STATUSES);

        if (alreadyInQueue) {
            throw new DuplicateResourceException(
                    "You already have an active entry in this queue");
        }

        int tokenNum = queue.nextTokenNumber();
        String tokenNumber = String.format("T-%03d", tokenNum);

        QueueEntry entry = new QueueEntry(queue, customer, tokenNumber);

        long peopleAhead = queueEntryRepository.countByQueueIdAndStatus(
                queueId, EntryStatus.WAITING);
        WaitTimeEstimationService.WaitEstimateResult waitInfo =
                waitTimeEstimationService.calculateWaitForJoin(queue, peopleAhead);
        entry.setEstimatedWaitMinutes(waitInfo.waitMinutes());
        entry.setEstimationSource(waitInfo.source());

        queueRepository.save(queue);
        QueueEntry saved = queueEntryRepository.save(entry);

        return QueueEntryResponse.from(saved);
    }

    /**
     * Get a customer's current position and wait estimate.
     */
    @Transactional(readOnly = true)
    public QueuePositionResponse getMyPosition(Long queueId, Long customerId) {
        QueueEntry entry = queueEntryRepository
                .findByQueueIdAndCustomerIdAndStatusIn(queueId, customerId, ACTIVE_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active entry found for you in this queue"));

        Queue queue = entry.getQueue();
        long ahead = queueEntryRepository.countWaitingAhead(queueId, entry.getJoinedAt());
        WaitTimeEstimateResponse estimate = waitTimeEstimationService.estimateWaitTime(entry);

        QueuePositionResponse response = new QueuePositionResponse();
        response.setTokenNumber(entry.getTokenNumber());
        response.setPosition((int) ahead + 1);
        response.setPeopleAhead((int) ahead);
        response.setStatus(entry.getStatus().name());
        response.setInitialEstimatedWaitMinutes(entry.getEstimatedWaitMinutes());
        response.setCurrentEstimatedWaitMinutes(estimate.getEstimatedWaitMinutes());
        response.setEstimatedWaitMinutes(estimate.getEstimatedWaitMinutes());
        response.setEstimationSource(estimate.getEstimationSource());
        response.setServiceName(queue.getService().getName());
        response.setBusinessName(queue.getService().getBusiness().getName());
        response.setQueueEntryId(entry.getId());
        return response;
    }

    @Transactional
    public QueueEntryResponse cancelEntry(Long entryId, Long customerId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue entry not found with id: " + entryId));

        if (!entry.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("You can only cancel your own queue entry");
        }

        entry.markCancelled();
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse callNextCustomer(Long queueId) {
        QueueEntry next = queueEntryRepository
                .findFirstByQueueIdAndStatusOrderByJoinedAtAsc(queueId, EntryStatus.WAITING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No waiting customers in this queue"));

        next.markCalled();
        QueueEntry saved = queueEntryRepository.save(next);
        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse startService(Long entryId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue entry not found with id: " + entryId));

        entry.markInService();
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse completeService(Long entryId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue entry not found with id: " + entryId));

        entry.markCompleted();
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.from(saved);
    }

    @Transactional
    public QueueEntryResponse skipEntry(Long entryId) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue entry not found with id: " + entryId));

        entry.markSkipped();
        QueueEntry saved = queueEntryRepository.save(entry);
        return QueueEntryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getQueueEntries(Long queueId) {
        queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Queue not found with id: " + queueId));

        return queueEntryRepository.findByQueueIdOrderByJoinedAtAsc(queueId)
                .stream()
                .map(QueueEntryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get real-time wait time estimate for a queue entry.
     */
    @Transactional(readOnly = true)
    public WaitTimeEstimateResponse getWaitTimeEstimate(Long entryId) {
        return waitTimeEstimationService.estimateWaitTime(entryId);
    }

    /**
     * Simple mathematical baseline for wait time estimation.
     * Delegates to WaitTimeEstimationService.
     */
    public int calculateBaselineWait(int peopleAhead,
                                      int avgServiceDurationMinutes,
                                      int activeCounters) {
        return waitTimeEstimationService.calculateBaselineWait(
                peopleAhead, avgServiceDurationMinutes, activeCounters);
    }
}
