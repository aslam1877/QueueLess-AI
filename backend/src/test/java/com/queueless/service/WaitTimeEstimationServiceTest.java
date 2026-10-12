package com.queueless.service;

import com.queueless.dto.response.WaitTimeEstimateResponse;
import com.queueless.entity.Business;
import com.queueless.entity.Queue;
import com.queueless.entity.QueueEntry;
import com.queueless.entity.ServiceEntity;
import com.queueless.entity.User;
import com.queueless.enums.EntryStatus;
import com.queueless.enums.UserRole;
import com.queueless.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitTimeEstimationServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private com.queueless.client.PredictionClient predictionClient;

    @InjectMocks
    private WaitTimeEstimationService waitTimeEstimationService;

    private User owner;
    private User customer;
    private Business business;
    private ServiceEntity service;
    private Queue queue;
    private QueueEntry entry;

    @BeforeEach
    void setUp() {
        owner = new User("owner@example.com", "hash", "Owner Name", UserRole.BUSINESS_OWNER);
        ReflectionTestUtils.setField(owner, "id", 1L);

        customer = new User("customer@example.com", "hash", "Customer Name", UserRole.CUSTOMER);
        ReflectionTestUtils.setField(customer, "id", 2L);

        business = new Business("Test Business", "Address", "1234567890", "Description", owner);
        ReflectionTestUtils.setField(business, "id", 10L);

        service = new ServiceEntity("General Consultation", "Description", 20, business);
        ReflectionTestUtils.setField(service, "id", 20L);

        queue = new Queue(service, LocalDate.now(), 2);
        ReflectionTestUtils.setField(queue, "id", 30L);

        entry = new QueueEntry(queue, customer, "T-001");
        ReflectionTestUtils.setField(entry, "id", 100L);
    }

    @Test
    @DisplayName("calculateBaselineWait: zero or negative people ahead should return 0")
    void testBaselineZeroPeopleAhead() {
        assertEquals(0, waitTimeEstimationService.calculateBaselineWait(0, 20, 2));
        assertEquals(0, waitTimeEstimationService.calculateBaselineWait(-1, 20, 2));
    }

    @Test
    @DisplayName("calculateBaselineWait: accurate ceiling calculation across multiple counters")
    void testBaselineCalculationFormula() {
        // 5 people * 20 min / 2 counters = 50 min
        assertEquals(50, waitTimeEstimationService.calculateBaselineWait(5, 20, 2));

        // 5 people * 20 min / 3 counters = ceil(33.33) = 34 min
        assertEquals(34, waitTimeEstimationService.calculateBaselineWait(5, 20, 3));

        // 1 person * 10 min / 1 counter = 10 min
        assertEquals(10, waitTimeEstimationService.calculateBaselineWait(1, 10, 1));
    }

    @Test
    @DisplayName("calculateBaselineWait: active counters <= 0 should fallback to 1")
    void testBaselineZeroCountersFallback() {
        // 3 people * 15 min / fallback 1 counter = 45 min
        assertEquals(45, waitTimeEstimationService.calculateBaselineWait(3, 15, 0));
        assertEquals(45, waitTimeEstimationService.calculateBaselineWait(3, 15, -2));
    }

    @Test
    @DisplayName("calculateBaselineWait: duration <= 0 should fallback to default 15 mins")
    void testBaselineZeroDurationFallback() {
        // 2 people * default 15 min / 1 counter = 30 min
        assertEquals(30, waitTimeEstimationService.calculateBaselineWait(2, 0, 1));
        assertEquals(30, waitTimeEstimationService.calculateBaselineWait(2, -5, 1));
    }

    @Test
    @DisplayName("estimateWaitTime: CALLED or IN_SERVICE entries have 0 min wait")
    void testEstimateCalledOrInService() {
        entry.markCalled();
        WaitTimeEstimateResponse calledResponse = waitTimeEstimationService.estimateWaitTime(entry);
        assertEquals(0, calledResponse.getEstimatedWaitMinutes());
        assertEquals(0, calledResponse.getPeopleAhead());
        assertEquals("CALLED", calledResponse.getStatus());

        entry.markInService();
        WaitTimeEstimateResponse inServiceResponse = waitTimeEstimationService.estimateWaitTime(entry);
        assertEquals(0, inServiceResponse.getEstimatedWaitMinutes());
        assertEquals("IN_SERVICE", inServiceResponse.getStatus());
    }

    @Test
    @DisplayName("estimateWaitTime: finished entries (COMPLETED, CANCELLED, SKIPPED) have 0 min wait")
    void testEstimateTerminalStatuses() {
        entry.markCalled();
        entry.markInService();
        entry.markCompleted();

        WaitTimeEstimateResponse completedResponse = waitTimeEstimationService.estimateWaitTime(entry);
        assertEquals(0, completedResponse.getEstimatedWaitMinutes());
        assertEquals("COMPLETED", completedResponse.getStatus());
        assertNull(completedResponse.getEstimatedServiceTime());
    }

    @Test
    @DisplayName("estimateWaitTime: WAITING entry with static baseline when insufficient completion history")
    void testEstimateWaitingEntryStaticBaseline() {
        when(queueEntryRepository.countWaitingAhead(eq(30L), any(LocalDateTime.class)))
                .thenReturn(3L);
        when(queueEntryRepository.findRecentCompletedByServiceId(eq(20L), any(Pageable.class)))
                .thenReturn(Collections.emptyList()); // No historical data

        WaitTimeEstimateResponse response = waitTimeEstimationService.estimateWaitTime(entry);

        assertNotNull(response);
        assertEquals(100L, response.getQueueEntryId());
        assertEquals("T-001", response.getTokenNumber());
        assertEquals("WAITING", response.getStatus());
        assertEquals(4, response.getPosition()); // 3 ahead + 1
        assertEquals(3, response.getPeopleAhead());
        assertEquals(2, response.getActiveCounters());
        assertEquals(20, response.getServiceDurationMinutes()); // Configured 20 min
        // 3 ahead * 20 min / 2 counters = ceil(30.0) = 30 min
        assertEquals(30, response.getEstimatedWaitMinutes());
        assertEquals("BASELINE", response.getEstimationSource());
        assertNotNull(response.getEstimatedServiceTime());
        assertTrue(response.getExplanation().contains("3 customer(s) ahead"));
    }

    @Test
    @DisplayName("resolveEffectiveDuration: uses empirical moving average when 3+ completed entries exist")
    void testEmpiricalMovingAverageCalculation() {
        LocalDateTime now = LocalDateTime.now();

        // Entry 1: 15 min duration
        QueueEntry e1 = new QueueEntry(queue, customer, "T-010");
        ReflectionTestUtils.setField(e1, "status", EntryStatus.COMPLETED);
        ReflectionTestUtils.setField(e1, "serviceStartedAt", now.minusMinutes(45));
        ReflectionTestUtils.setField(e1, "serviceCompletedAt", now.minusMinutes(30));

        // Entry 2: 25 min duration
        QueueEntry e2 = new QueueEntry(queue, customer, "T-011");
        ReflectionTestUtils.setField(e2, "status", EntryStatus.COMPLETED);
        ReflectionTestUtils.setField(e2, "serviceStartedAt", now.minusMinutes(30));
        ReflectionTestUtils.setField(e2, "serviceCompletedAt", now.minusMinutes(5));

        // Entry 3: 20 min duration
        QueueEntry e3 = new QueueEntry(queue, customer, "T-012");
        ReflectionTestUtils.setField(e3, "status", EntryStatus.COMPLETED);
        ReflectionTestUtils.setField(e3, "serviceStartedAt", now.minusMinutes(50));
        ReflectionTestUtils.setField(e3, "serviceCompletedAt", now.minusMinutes(30));

        when(queueEntryRepository.findRecentCompletedByServiceId(eq(20L), any(Pageable.class)))
                .thenReturn(List.of(e1, e2, e3));

        WaitTimeEstimationService.EffectiveDuration result =
                waitTimeEstimationService.resolveEffectiveDuration(queue);

        // Average of (15 + 25 + 20) / 3 = 60 / 3 = 20 mins
        assertEquals(20, result.durationMinutes());
        assertEquals("BASELINE_EMPIRICAL", result.source());
    }

    @Test
    @DisplayName("estimateWaitTime by ID fetches entry and calculates correctly")
    void testEstimateWaitTimeById() {
        when(queueEntryRepository.findById(100L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.countWaitingAhead(eq(30L), any(LocalDateTime.class))).thenReturn(1L);
        when(queueEntryRepository.findRecentCompletedByServiceId(eq(20L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        WaitTimeEstimateResponse response = waitTimeEstimationService.estimateWaitTime(100L);

        assertEquals(100L, response.getQueueEntryId());
        assertEquals(1, response.getPeopleAhead());
        // 1 * 20 / 2 = 10 min
        assertEquals(10, response.getEstimatedWaitMinutes());
    }

    @Test
    @DisplayName("estimateWaitTime: Uses ML prediction when Python microservice succeeds")
    void testEstimateWaitingEntryWithMLModel() {
        when(queueEntryRepository.countWaitingAhead(eq(30L), any(LocalDateTime.class))).thenReturn(4L);
        when(queueEntryRepository.findRecentCompletedByServiceId(eq(20L), any(Pageable.class))).thenReturn(Collections.emptyList());

        com.queueless.client.dto.PredictionResponse mlResponse = new com.queueless.client.dto.PredictionResponse();
        mlResponse.setPredictedWaitMinutes(52); // Baseline would be 4 * 20 / 2 = 40, ML predicts 52 (rush hour)
        mlResponse.setBaselineWaitMinutes(40);
        mlResponse.setDifferenceMinutes(12);
        mlResponse.setEstimationSource("ML_MODEL");
        mlResponse.setModelName("Gradient Boosting");

        when(predictionClient.predictWaitTime(any())).thenReturn(Optional.of(mlResponse));

        WaitTimeEstimateResponse response = waitTimeEstimationService.estimateWaitTime(entry);

        assertNotNull(response);
        assertEquals(52, response.getEstimatedWaitMinutes());
        assertEquals("ML_MODEL", response.getEstimationSource());
        assertTrue(response.getExplanation().contains("ML predicted 52 min wait"));
        assertTrue(response.getExplanation().contains("Adjusted by +12 min(s)"));
    }

    @Test
    @DisplayName("calculateWaitForJoin: Uses ML prediction when joining queue")
    void testCalculateWaitForJoinWithML() {
        com.queueless.client.dto.PredictionResponse mlResponse = new com.queueless.client.dto.PredictionResponse();
        mlResponse.setPredictedWaitMinutes(48);
        mlResponse.setEstimationSource("ML_MODEL");

        when(predictionClient.predictWaitTime(any())).thenReturn(Optional.of(mlResponse));

        WaitTimeEstimationService.WaitEstimateResult result =
                waitTimeEstimationService.calculateWaitForJoin(queue, 3L);

        assertEquals(48, result.waitMinutes());
        assertEquals("ML_MODEL", result.source());
    }

    @Test
    @DisplayName("calculateWaitForJoin: Gracefully falls back to baseline when ML offline")
    void testCalculateWaitForJoinFallback() {
        when(predictionClient.predictWaitTime(any())).thenReturn(Optional.empty());

        WaitTimeEstimationService.WaitEstimateResult result =
                waitTimeEstimationService.calculateWaitForJoin(queue, 3L);

        // Baseline: ceil(3 * 20 / 2) = 30 mins
        assertEquals(30, result.waitMinutes());
        assertEquals("BASELINE", result.source());
    }
}
