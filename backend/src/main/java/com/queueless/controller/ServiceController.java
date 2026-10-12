package com.queueless.controller;

import com.queueless.dto.request.CreateServiceRequest;
import com.queueless.dto.response.ServiceResponse;
import com.queueless.security.UserPrincipal;
import com.queueless.service.ServiceManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for service management within a business.
 */
@RestController
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    /**
     * Create a service for a business.
     * POST /api/businesses/1/services
     */
    @PostMapping("/api/businesses/{businessId}/services")
    public ResponseEntity<ServiceResponse> createService(
            @PathVariable Long businessId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ownerId,
            @Valid @RequestBody CreateServiceRequest request) {
        Long effectiveOwnerId = (principal != null) ? principal.getId() : ownerId;
        if (effectiveOwnerId == null) {
            throw new IllegalArgumentException("Owner identity could not be determined from authentication context or request parameter");
        }
        ServiceResponse response = serviceManagementService.createService(
                businessId, effectiveOwnerId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * List all services for a business.
     * GET /api/businesses/1/services
     */
    @GetMapping("/api/businesses/{businessId}/services")
    public ResponseEntity<List<ServiceResponse>> getServicesByBusiness(
            @PathVariable Long businessId) {
        List<ServiceResponse> responses = serviceManagementService
                .getServicesByBusiness(businessId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a single service by ID.
     * GET /api/services/5
     */
    @GetMapping("/api/services/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        ServiceResponse response = serviceManagementService.getServiceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a service.
     * PUT /api/services/5
     */
    @PutMapping("/api/services/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ownerId,
            @Valid @RequestBody CreateServiceRequest request) {
        Long effectiveOwnerId = (principal != null) ? principal.getId() : ownerId;
        if (effectiveOwnerId == null) {
            throw new IllegalArgumentException("Owner identity could not be determined from authentication context or request parameter");
        }
        ServiceResponse response = serviceManagementService.updateService(
                id, effectiveOwnerId, request);
        return ResponseEntity.ok(response);
    }
}
