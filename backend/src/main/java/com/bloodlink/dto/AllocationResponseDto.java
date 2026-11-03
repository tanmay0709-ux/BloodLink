package com.bloodlink.dto;

import com.bloodlink.model.BloodAllocation;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodUnit;

import java.time.LocalDateTime;

public class AllocationResponseDto {

    private Long id;
    private Long requestId;
    private Long bloodUnitDbId;
    private String unitId;
    private BloodInventory.BloodType bloodType;
    private BloodUnit.BloodComponentType componentType;
    private Integer quantity;
    private Long bloodBankId;
    private String bloodBankName;
    private BloodAllocation.AllocationStatus status;
    private LocalDateTime allocatedAt;
    private String allocatedByUsername;

    public AllocationResponseDto() {}

    public static AllocationResponseDto fromEntity(BloodAllocation a) {
        AllocationResponseDto dto = new AllocationResponseDto();
        dto.setId(a.getId());
        if (a.getRequest() != null) {
            dto.setRequestId(a.getRequest().getId());
        }
        if (a.getBloodUnit() != null) {
            dto.setBloodUnitDbId(a.getBloodUnit().getId());
            dto.setUnitId(a.getBloodUnit().getUnitId());
            dto.setBloodType(a.getBloodUnit().getBloodType());
            dto.setComponentType(a.getBloodUnit().getComponentType());
            dto.setQuantity(a.getBloodUnit().getQuantity());
        }
        if (a.getBloodBank() != null) {
            dto.setBloodBankId(a.getBloodBank().getId());
            dto.setBloodBankName(a.getBloodBank().getName());
        }
        dto.setStatus(a.getStatus());
        dto.setAllocatedAt(a.getAllocatedAt());
        if (a.getAllocatedBy() != null) {
            dto.setAllocatedByUsername(a.getAllocatedBy().getUsername());
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getBloodUnitDbId() { return bloodUnitDbId; }
    public void setBloodUnitDbId(Long bloodUnitDbId) { this.bloodUnitDbId = bloodUnitDbId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public BloodInventory.BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodInventory.BloodType bloodType) { this.bloodType = bloodType; }

    public BloodUnit.BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodUnit.BloodComponentType componentType) { this.componentType = componentType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }

    public String getBloodBankName() { return bloodBankName; }
    public void setBloodBankName(String bloodBankName) { this.bloodBankName = bloodBankName; }

    public BloodAllocation.AllocationStatus getStatus() { return status; }
    public void setStatus(BloodAllocation.AllocationStatus status) { this.status = status; }

    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }

    public String getAllocatedByUsername() { return allocatedByUsername; }
    public void setAllocatedByUsername(String allocatedByUsername) { this.allocatedByUsername = allocatedByUsername; }
}
