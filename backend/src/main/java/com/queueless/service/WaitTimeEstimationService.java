package com.queueless.service;

import com.queueless.client.PredictionClient;
import com.queueless.client.dto.PredictionRequest;
import com.queueless.client.dto.PredictionResponse;
import com.queueless.dto.response.WaitTimeEstimateResponse;
import com.queueless.entity.Queue;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.ServiceEntity;
import com.queueless.enums.EntryStatus;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.QueueEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Computes estimated wait times using active counter capacity, moving averages,
 * and ML predictions with deterministic fallback.
 */
@Service
public class WaitTimeEstimationService {

    private static final Logger log = LoggerFactory.getLogger(WaitTimeEstimationService.class);

    private static final int MIN_HISTORICAL_SAMPLES_FOR_EMPIRICAL = 3;
    private static final int MAX_HISTORICAL_SAMPLES_TO_CONSIDER = 10;
    private static final int DEFAULT_FALLBACK_DURATION_MINUTES = 15;

    private final QueueEntryRepository queueEntryRepository;
    private final PredictionClient predictionClient;

    public WaitTimeEstimationService(QueueEntryRepository queueEntryRepository,
                                     PredictionClient predictionClient) {
        this.queueEntryRepository = queueEntryRepository;
        this.predictionClient = predictionClient;
    }

    /**
     * Calculates the baseline wait time using deterministic queueing formula:
     * ceil((peopleAhead * avgDuration) / activeCounters)
     *
     * @param peopleAhead number of waiting customers ahead in queue
     * @param avgServiceDurationMinutes duration per customer in minutes
     * @param activeCounters number of operational counters / service points
     * @return estimated wait time in integer minutes
     */
    public int calculateBaselineWait(int peopleAhead,
                                     int avgServiceDurationMinutes,
                                     int activeCounters) {
        if (peopleAhead <= 0) {
            return 0;
        }

        int counters = (activeCounters <= 0) ? 1 : activeCounters;
        int duration = (avgServiceDurationMinutes <= 0)
                ? DEFAULT_FALLBACK_DURATION_MINUTES
                : avgServiceDurationMinutes;

        return (int) Math.ceil((double) (peopleAhead * duration) / counters);
    }

    /**
     * Determines the effective service duration and estimation source.
     * If at least 3 completed entries exist with valid timestamps, uses the empirical
     * moving average of actual completed service times. Otherwise, falls back to the
     * statically configured duration on the service entity.
     */
    public EffectiveDuration resolveEffectiveDuration(Queue queue) {
        ServiceEntity service = queue.getService();
        int configuredDuration = (service != null && service.getEstimatedDurationMinutes() > 0)
                ? service.getEstimatedDurationMinutes()
                : DEFAULT_FALLBACK_DURATION_MINUTES;

        if (service == null || service.getId() == null) {
            return new EffectiveDuration(configuredDuration, "BASELINE");
        }

        // Fetch recent completed entries for this service
        List<QueueEntry> recentCompleted = queueEntryRepository.findRecentCompletedByServiceId(
                service.getId(),
                PageRequest.of(0, MAX_HISTORICAL_SAMPLES_TO_CONSIDER)
        );

        if (recentCompleted.size() >= MIN_HISTORICAL_SAMPLES_FOR_EMPIRICAL) {
            long totalMinutes = 0;
            int validCount = 0;

            for (QueueEntry entry : recentCompleted) {
                if (entry.getServiceStartedAt() != null && entry.getServiceCompletedAt() != null) {
                    long minutes = Duration.between(entry.getServiceStartedAt(), entry.getServiceCompletedAt()).toMinutes();
                    // Sanity check duration between 1 minute and 300 minutes (5 hours)
                    if (minutes > 0 && minutes <= 300) {
                        totalMinutes += minutes;
                        validCount++;
                    }
                }
            }

            if (validCount >= MIN_HISTORICAL_SAMPLES_FOR_EMPIRICAL) {
                int movingAverage = (int) Math.round((double) totalMinutes / validCount);
                if (movingAverage <= 0) movingAverage = 1;
                log.debug("Using empirical moving average duration of {} mins based on {} samples for service {}",
                        movingAverage, validCount, service.getId());
                return new EffectiveDuration(movingAverage, "BASELINE_EMPIRICAL");
            }
        }

        return new EffectiveDuration(configuredDuration, "BASELINE");
    }

    /**
     * Estimate wait time for an existing QueueEntry by entry ID.
     */
    @Transactional(readOnly = true)
    public WaitTimeEstimateResponse estimateWaitTime(Long queueEntryId) {
        QueueEntry entry = queueEntryRepository.findById(queueEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + queueEntryId));
        return estimateWaitTime(entry);
    }

    /**
     * Calculates the real-time wait estimate for a queue entry based on current queue conditions.
     */
    @Transactional(readOnly = true)
    public WaitTimeEstimateResponse estimateWaitTime(QueueEntry entry) {
        Queue queue = entry.getQueue();
        EntryStatus status = entry.getStatus();
        int activeCounters = (queue.getActiveCounters() <= 0) ? 1 : queue.getActiveCounters();

        if (status == EntryStatus.CALLED || status == EntryStatus.IN_SERVICE) {
            return new WaitTimeEstimateResponse(
                    entry.getId(),
                    entry.getTokenNumber(),
                    status.name(),
                    0,
                    0,
                    activeCounters,
                    queue.getService().getEstimatedDurationMinutes(),
                    0,
                    entry.getEstimationSource() != null ? entry.getEstimationSource() : "BASELINE",
                    LocalDateTime.now(),
                    "Customer has already been called or is currently receiving service."
            );
        }

        if (status == EntryStatus.COMPLETED || status == EntryStatus.CANCELLED || status == EntryStatus.SKIPPED) {
            return new WaitTimeEstimateResponse(
                    entry.getId(),
                    entry.getTokenNumber(),
                    status.name(),
                    0,
                    0,
                    activeCounters,
                    queue.getService().getEstimatedDurationMinutes(),
                    0,
                    entry.getEstimationSource() != null ? entry.getEstimationSource() : "BASELINE",
                    null,
                    "Queue entry is finished (Status: " + status.name() + ")."
            );
        }

        long peopleAhead = queueEntryRepository.countWaitingAhead(queue.getId(), entry.getJoinedAt());
        int position = (int) peopleAhead + 1;

        EffectiveDuration durationInfo = resolveEffectiveDuration(queue);
        int baselineMinutes = calculateBaselineWait((int) peopleAhead, durationInfo.durationMinutes(), activeCounters);

        // Attempt ML prediction via Python microservice
        LocalDateTime now = LocalDateTime.now();
        PredictionRequest mlRequest = new PredictionRequest(
                (int) peopleAhead,
                activeCounters,
                (double) durationInfo.durationMinutes(),
                now.getHour(),
                now.getDayOfWeek().getValue() - 1, // 0=Mon, 6=Sun
                queue.getService().getName()
        );

        Optional<PredictionResponse> mlResponse = predictionClient.predictWaitTime(mlRequest);

        int finalEstimatedMinutes;
        String finalSource;
        String explanation;

        if (mlResponse.isPresent() && mlResponse.get().getPredictedWaitMinutes() != null) {
            PredictionResponse pred = mlResponse.get();
            finalEstimatedMinutes = pred.getPredictedWaitMinutes();
            finalSource = "ML_MODEL";
            int diff = (pred.getDifferenceMinutes() != null) ? pred.getDifferenceMinutes() : (finalEstimatedMinutes - baselineMinutes);
            explanation = String.format(
                    "ML predicted %d min wait: %d people ahead across %d counter(s) (baseline: %d min). Adjusted by %+d min(s) for rush/demand factors using %s.",
                    finalEstimatedMinutes,
                    peopleAhead,
                    activeCounters,
                    baselineMinutes,
                    diff,
                    pred.getModelName() != null ? pred.getModelName() : "Gradient Boosting"
            );
        } else {
            // Graceful fallback to deterministic formula
            finalEstimatedMinutes = baselineMinutes;
            finalSource = durationInfo.source();
            explanation = String.format(
                    "Estimated %d min wait (deterministic fallback): %d customer(s) ahead with %d active counter(s) and %d min %s duration.",
                    finalEstimatedMinutes,
                    peopleAhead,
                    activeCounters,
                    durationInfo.durationMinutes(),
                    "BASELINE_EMPIRICAL".equals(durationInfo.source()) ? "empirical average" : "estimated"
            );
        }

        LocalDateTime expectedServiceTime = LocalDateTime.now().plusMinutes(finalEstimatedMinutes);

        return new WaitTimeEstimateResponse(
                entry.getId(),
                entry.getTokenNumber(),
                status.name(),
                position,
                (int) peopleAhead,
                activeCounters,
                durationInfo.durationMinutes(),
                finalEstimatedMinutes,
                finalSource,
                expectedServiceTime,
                explanation
        );
    }

    /**
     * Calculates wait estimate and source when a customer first joins a queue.
     * Tries ML prediction first, then seamlessly falls back to baseline math.
     */
    public WaitEstimateResult calculateWaitForJoin(Queue queue, long peopleAhead) {
        EffectiveDuration durationInfo = resolveEffectiveDuration(queue);
        int activeCounters = (queue.getActiveCounters() <= 0) ? 1 : queue.getActiveCounters();
        int baselineMinutes = calculateBaselineWait((int) peopleAhead, durationInfo.durationMinutes(), activeCounters);

        if (peopleAhead <= 0) {
            return new WaitEstimateResult(0, "BASELINE");
        }

        LocalDateTime now = LocalDateTime.now();
        PredictionRequest mlRequest = new PredictionRequest(
                (int) peopleAhead,
                activeCounters,
                (double) durationInfo.durationMinutes(),
                now.getHour(),
                now.getDayOfWeek().getValue() - 1,
                queue.getService().getName()
        );

        Optional<PredictionResponse> mlResponse = predictionClient.predictWaitTime(mlRequest);
        if (mlResponse.isPresent() && mlResponse.get().getPredictedWaitMinutes() != null) {
            return new WaitEstimateResult(mlResponse.get().getPredictedWaitMinutes(), "ML_MODEL");
        }

        return new WaitEstimateResult(baselineMinutes, durationInfo.source());
    }

    /**
     * Helper record holding wait minutes and source for joining customers.
     */
    public record WaitEstimateResult(int waitMinutes, String source) {}

    /**
     * Helper record holding calculated duration and source label.
     */
    public record EffectiveDuration(int durationMinutes, String source) {}
}
