package com.queueless.repository;

import com.queueless.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    /** Find all businesses owned by a specific user. */
    List<Business> findByOwnerId(Long ownerId);

    /** Find a business by its owner's ID (for single-business owners). */
    Optional<Business> findFirstByOwnerId(Long ownerId);

    /** Check if a user already owns a business. */
    boolean existsByOwnerId(Long ownerId);
}
