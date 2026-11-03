package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodRequest;
import com.bloodlink.model.BloodUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class BloodRequestCreateDto {

    @NotNull(message = "Blood group is required")
    private BloodInventory.BloodType bloodType;

    @NotNull(message = "Component type is required")
    private BloodUnit.BloodComponentType componentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1 unit")
    private Integer quantity;

    @NotNull(message = "Priority is required")
    private BloodRequest.RequestPriority priority;

    @NotNull(message = "Required date is required")
    private LocalDate requiredDate;

    @Size(max = 500, message = "Clinical reason cannot exceed 500 characters")
    private String clinicalReason;

    private Long hospitalId; // Optional for admin, automatic for hospital staff

    public BloodRequestCreateDto() {}

    public BloodInventory.BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodInventory.BloodType bloodType) { this.bloodType = bloodType; }

    public BloodUnit.BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodUnit.BloodComponentType componentType) { this.componentType = componentType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BloodRequest.RequestPriority getPriority() { return priority; }
    public void setPriority(BloodRequest.RequestPriority priority) { this.priority = priority; }

    public LocalDate getRequiredDate() { return requiredDate; }
    public void setRequiredDate(LocalDate requiredDate) { this.requiredDate = requiredDate; }

    public String getClinicalReason() { return clinicalReason; }
    public void setClinicalReason(String clinicalReason) { this.clinicalReason = clinicalReason; }

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
}
