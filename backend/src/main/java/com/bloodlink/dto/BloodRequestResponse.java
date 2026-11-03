package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodRequest;
import com.bloodlink.model.BloodUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BloodRequestResponse {

    private Long id;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalAddress;
    private BloodInventory.BloodType bloodType;
    private BloodUnit.BloodComponentType componentType;
    private String componentDisplayName;
    private Integer quantity;
    private BloodRequest.RequestPriority priority;
    private LocalDate requiredDate;
    private BloodRequest.RequestStatus status;
    private String createdByUsername;
    private String clinicalReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BloodRequestResponse() {}

    public static BloodRequestResponse fromEntity(BloodRequest req) {
        BloodRequestResponse res = new BloodRequestResponse();
        res.setId(req.getId());
        if (req.getHospital() != null) {
            res.setHospitalId(req.getHospital().getId());
            res.setHospitalName(req.getHospital().getName());
            res.setHospitalAddress(req.getHospital().getAddress());
        }
        res.setBloodType(req.getBloodType());
        res.setComponentType(req.getComponentType());
        if (req.getComponentType() != null) {
            res.setComponentDisplayName(req.getComponentType().getDisplayName());
        }
        res.setQuantity(req.getQuantity());
        res.setPriority(req.getPriority());
        res.setRequiredDate(req.getRequiredDate());
        res.setStatus(req.getStatus());
        if (req.getCreatedBy() != null) {
            res.setCreatedByUsername(req.getCreatedBy().getUsername());
        }
        res.setClinicalReason(req.getClinicalReason());
        res.setCreatedAt(req.getCreatedAt());
        res.setUpdatedAt(req.getUpdatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getHospitalAddress() { return hospitalAddress; }
    public void setHospitalAddress(String hospitalAddress) { this.hospitalAddress = hospitalAddress; }

    public BloodInventory.BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodInventory.BloodType bloodType) { this.bloodType = bloodType; }

    public BloodUnit.BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodUnit.BloodComponentType componentType) { this.componentType = componentType; }

    public String getComponentDisplayName() { return componentDisplayName; }
    public void setComponentDisplayName(String componentDisplayName) { this.componentDisplayName = componentDisplayName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BloodRequest.RequestPriority getPriority() { return priority; }
    public void setPriority(BloodRequest.RequestPriority priority) { this.priority = priority; }

    public LocalDate getRequiredDate() { return requiredDate; }
    public void setRequiredDate(LocalDate requiredDate) { this.requiredDate = requiredDate; }

    public BloodRequest.RequestStatus getStatus() { return status; }
    public void setStatus(BloodRequest.RequestStatus status) { this.status = status; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public String getClinicalReason() { return clinicalReason; }
    public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
