package com.queueless.service;

import com.queueless.dto.request.CreateBusinessRequest;
import com.queueless.dto.response.BusinessResponse;
import com.queueless.entity.Business;
import com.queueless.entity.User;
import com.queueless.enums.UserRole;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.BusinessRepository;
import com.queueless.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing business entities and owner configurations.
 */
@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public BusinessService(BusinessRepository businessRepository,
                           UserRepository userRepository) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new business for a BUSINESS_OWNER user.
     *
     * Rules:
     * 1. The user must exist.
     * 2. The user must have BUSINESS_OWNER role.
     * 3. The user must not already own a business (MVP: one business per owner).
     */
    @Transactional
    public BusinessResponse createBusiness(Long ownerId, CreateBusinessRequest request) {
        // Find the owner
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + ownerId));

        // Verify role
        if (owner.getRole() != UserRole.BUSINESS_OWNER) {
            throw new IllegalArgumentException(
                    "Only users with BUSINESS_OWNER role can create a business. " +
                    "Current role: " + owner.getRole());
        }

        // Check for existing business (MVP: one per owner)
        if (businessRepository.existsByOwnerId(ownerId)) {
            throw new DuplicateResourceException(
                    "User already owns a business. Each owner can have one business in the current version.");
        }

        Business business = new Business(
                request.getName(),
                request.getAddress(),
                request.getPhone(),
                request.getDescription(),
                owner
        );

        Business saved = businessRepository.save(business);
        return BusinessResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BusinessResponse getBusinessById(Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business not found with id: " + id));
        return BusinessResponse.from(business);
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> getBusinessesByOwner(Long ownerId) {
        List<Business> businesses = businessRepository.findByOwnerId(ownerId);
        return businesses.stream()
                .map(BusinessResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Updates business details. Only the owner can update their business.
     */
    @Transactional
    public BusinessResponse updateBusiness(Long businessId, Long ownerId,
                                           CreateBusinessRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business not found with id: " + businessId));

        // Authorization check: only the owner can update
        if (!business.getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to update this business");
        }

        business.setName(request.getName());
        business.setAddress(request.getAddress());
        business.setPhone(request.getPhone());
        business.setDescription(request.getDescription());

        Business updated = businessRepository.save(business);
        return BusinessResponse.from(updated);
    }

    /**
     * Returns the Business entity (for internal service-to-service use).
     * Not exposed to controllers — they use DTOs.
     */
    @Transactional(readOnly = true)
    public Business getBusinessEntityById(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Business not found with id: " + id));
    }
}
