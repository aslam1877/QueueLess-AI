package com.queueless.service;

import com.queueless.dto.request.CreateServiceRequest;
import com.queueless.dto.response.ServiceResponse;
import com.queueless.entity.Business;
import com.queueless.entity.ServiceEntity;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for managing services within a business.
 *
 * A "service" here is something the business offers to customers,
 * like "Haircut" (30 min) or "Screen Replacement" (45 min).
 */
@Service
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final BusinessService businessService;

    public ServiceManagementService(ServiceRepository serviceRepository,
                                     BusinessService businessService) {
        this.serviceRepository = serviceRepository;
        this.businessService = businessService;
    }

    /**
     * Creates a new service for a business.
     *
     * @param businessId the business offering this service
     * @param ownerId the ID of the user making the request (for authorization)
     * @param request the service details
     */
    @Transactional
    public ServiceResponse createService(Long businessId, Long ownerId,
                                          CreateServiceRequest request) {
        Business business = businessService.getBusinessEntityById(businessId);

        // Authorization: only the business owner can add services
        if (!business.getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to add services to this business");
        }

        ServiceEntity service = new ServiceEntity(
                request.getName(),
                request.getDescription(),
                request.getEstimatedDurationMinutes(),
                business
        );

        ServiceEntity saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByBusiness(Long businessId) {
        // Verify business exists
        businessService.getBusinessEntityById(businessId);

        List<ServiceEntity> services = serviceRepository.findByBusinessId(businessId);
        return services.stream()
                .map(ServiceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));
        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse updateService(Long serviceId, Long ownerId,
                                          CreateServiceRequest request) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + serviceId));

        // Authorization check
        if (!service.getBusiness().getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to update this service");
        }

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());

        ServiceEntity updated = serviceRepository.save(service);
        return ServiceResponse.from(updated);
    }

    /**
     * Returns the ServiceEntity (for internal use by QueueService).
     */
    @Transactional(readOnly = true)
    public ServiceEntity getServiceEntityById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service not found with id: " + id));
    }
}
