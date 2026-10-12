package com.queueless.controller;

import com.queueless.dto.request.CreateBusinessRequest;
import com.queueless.dto.response.BusinessResponse;
import com.queueless.security.UserPrincipal;
import com.queueless.service.BusinessService;
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
 * REST controller for business management.
 *
 * AUTHENTICATION:
 * Uses @AuthenticationPrincipal to automatically extract the authenticated
 * Business Owner's identity from the JWT token. Fallback to @RequestParam
 * allows seamless testing.
 */
@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * Create a new business.
     * POST /api/businesses
     */
    @PostMapping
    public ResponseEntity<BusinessResponse> createBusiness(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ownerId,
            @Valid @RequestBody CreateBusinessRequest request) {
        Long effectiveOwnerId = (principal != null) ? principal.getId() : ownerId;
        if (effectiveOwnerId == null) {
            throw new IllegalArgumentException("Owner identity could not be determined from authentication context or request parameter");
        }
        BusinessResponse response = businessService.createBusiness(effectiveOwnerId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get a business by ID.
     * GET /api/businesses/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessResponse> getBusinessById(@PathVariable Long id) {
        BusinessResponse response = businessService.getBusinessById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all businesses owned by the current owner.
     * GET /api/businesses
     */
    @GetMapping
    public ResponseEntity<List<BusinessResponse>> getBusinessesByOwner(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ownerId) {
        Long effectiveOwnerId = (principal != null) ? principal.getId() : ownerId;
        if (effectiveOwnerId == null) {
            throw new IllegalArgumentException("Owner identity could not be determined from authentication context or request parameter");
        }
        List<BusinessResponse> responses = businessService.getBusinessesByOwner(effectiveOwnerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update a business.
     * PUT /api/businesses/1
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusinessResponse> updateBusiness(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long ownerId,
            @Valid @RequestBody CreateBusinessRequest request) {
        Long effectiveOwnerId = (principal != null) ? principal.getId() : ownerId;
        if (effectiveOwnerId == null) {
            throw new IllegalArgumentException("Owner identity could not be determined from authentication context or request parameter");
        }
        BusinessResponse response = businessService.updateBusiness(id, effectiveOwnerId, request);
        return ResponseEntity.ok(response);
    }
}
