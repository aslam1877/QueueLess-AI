package com.queueless.controller;

import com.queueless.dto.request.OpenQueueRequest;
import com.queueless.dto.response.QueueEntryResponse;
import com.queueless.dto.response.QueuePositionResponse;
import com.queueless.dto.response.QueueResponse;
import com.queueless.dto.response.WaitTimeEstimateResponse;
import com.queueless.security.UserPrincipal;
import com.queueless.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for queue and entry lifecycle management.
 */
@RestController
@RequestMapping("/api")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/services/{serviceId}/queues")
    public ResponseEntity<QueueResponse> openQueue(
            @PathVariable Long serviceId,
            @Valid @RequestBody OpenQueueRequest request) {
        QueueResponse response = queueService.openQueue(serviceId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/queues/{id}")
    public ResponseEntity<QueueResponse> getQueue(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.getQueueById(id));
    }

    @PutMapping("/queues/{id}/close")
    public ResponseEntity<QueueResponse> closeQueue(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.closeQueue(id));
    }



    @PostMapping("/queues/{queueId}/entries")
    public ResponseEntity<QueueEntryResponse> joinQueue(
            @PathVariable Long queueId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long effectiveCustomerId = (principal != null) ? principal.getId() : customerId;
        if (effectiveCustomerId == null) {
            throw new IllegalArgumentException("Customer identity could not be determined from authentication context or request parameter");
        }
        QueueEntryResponse response = queueService.joinQueue(queueId, effectiveCustomerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/queues/{queueId}/entries/my")
    public ResponseEntity<QueuePositionResponse> getMyPosition(
            @PathVariable Long queueId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long effectiveCustomerId = (principal != null) ? principal.getId() : customerId;
        if (effectiveCustomerId == null) {
            throw new IllegalArgumentException("Customer identity could not be determined from authentication context or request parameter");
        }
        return ResponseEntity.ok(queueService.getMyPosition(queueId, effectiveCustomerId));
    }

    @PutMapping("/queue-entries/{id}/cancel")
    public ResponseEntity<QueueEntryResponse> cancelEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long effectiveCustomerId = (principal != null) ? principal.getId() : customerId;
        if (effectiveCustomerId == null) {
            throw new IllegalArgumentException("Customer identity could not be determined from authentication context or request parameter");
        }
        return ResponseEntity.ok(queueService.cancelEntry(id, effectiveCustomerId));
    }



    @PutMapping("/queues/{queueId}/entries/call-next")
    public ResponseEntity<QueueEntryResponse> callNext(
            @PathVariable Long queueId) {
        return ResponseEntity.ok(queueService.callNextCustomer(queueId));
    }

    @PutMapping("/queue-entries/{id}/start-service")
    public ResponseEntity<QueueEntryResponse> startService(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.startService(id));
    }

    @PutMapping("/queue-entries/{id}/complete")
    public ResponseEntity<QueueEntryResponse> completeService(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.completeService(id));
    }

    @PutMapping("/queue-entries/{id}/skip")
    public ResponseEntity<QueueEntryResponse> skipEntry(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.skipEntry(id));
    }

    @GetMapping("/queues/{queueId}/entries")
    public ResponseEntity<List<QueueEntryResponse>> getQueueEntries(
            @PathVariable Long queueId) {
        return ResponseEntity.ok(queueService.getQueueEntries(queueId));
    }



    @GetMapping("/queue-entries/{id}/estimate")
    public ResponseEntity<WaitTimeEstimateResponse> getWaitTimeEstimate(@PathVariable Long id) {
        return ResponseEntity.ok(queueService.getWaitTimeEstimate(id));
    }
}
