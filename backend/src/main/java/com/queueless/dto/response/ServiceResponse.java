package com.queueless.dto.response;

import com.queueless.entity.ServiceEntity;
import java.time.LocalDateTime;

public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private Integer estimatedDurationMinutes;
    private Boolean active;
    private Long businessId;
    private String businessName;
    private LocalDateTime createdAt;

    public ServiceResponse() {}

    public static ServiceResponse from(ServiceEntity service) {
        ServiceResponse response = new ServiceResponse();
        response.id = service.getId();
        response.name = service.getName();
        response.description = service.getDescription();
        response.estimatedDurationMinutes = service.getEstimatedDurationMinutes();
        response.active = service.getActive();
        response.businessId = service.getBusiness().getId();
        response.businessName = service.getBusiness().getName();
        response.createdAt = service.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public Boolean getActive() { return active; }
    public Long getBusinessId() { return businessId; }
    public String getBusinessName() { return businessName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
