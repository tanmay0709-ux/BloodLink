package com.bloodlink.dto;

import com.bloodlink.model.BloodUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class BloodUnitRequest {

    @Size(max = 50, message = "Unit ID cannot exceed 50 characters")
    private String unitId;

    @NotNull(message = "Donation ID is required")
    private Long donationId;

    @NotNull(message = "Component type is required")
    private BloodUnit.BloodComponentType componentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    private LocalDate collectionDate;

    private LocalDate expiryDate;

    private BloodUnit.BloodUnitStatus status;

    @Size(max = 500)
    private String notes;

    private Long bloodBankId; // Optional for admin, automatic for staff

    public BloodUnitRequest() {}

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public Long getDonationId() { return donationId; }
    public void setDonationId(Long donationId) { this.donationId = donationId; }

    public BloodUnit.BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodUnit.BloodComponentType componentType) { this.componentType = componentType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public BloodUnit.BloodUnitStatus getStatus() { return status; }
    public void setStatus(BloodUnit.BloodUnitStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }
}
