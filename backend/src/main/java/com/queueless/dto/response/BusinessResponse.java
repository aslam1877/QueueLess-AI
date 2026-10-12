package com.queueless.dto.response;

import com.queueless.entity.Business;
import java.time.LocalDateTime;

public class BusinessResponse {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private String description;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;

    public BusinessResponse() {}

    public static BusinessResponse from(Business business) {
        BusinessResponse response = new BusinessResponse();
        response.id = business.getId();
        response.name = business.getName();
        response.address = business.getAddress();
        response.phone = business.getPhone();
        response.description = business.getDescription();
        response.ownerId = business.getOwner().getId();
        response.ownerName = business.getOwner().getName();
        response.createdAt = business.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getDescription() { return description; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
