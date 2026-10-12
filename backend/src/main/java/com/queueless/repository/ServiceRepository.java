package com.queueless.repository;

import com.queueless.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    /** Find all services offered by a business. */
    List<ServiceEntity> findByBusinessId(Long businessId);

    /** Find only active services for a business. */
    List<ServiceEntity> findByBusinessIdAndActiveTrue(Long businessId);
}
