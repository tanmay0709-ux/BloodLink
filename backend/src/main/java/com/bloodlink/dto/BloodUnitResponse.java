package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BloodUnitResponse {

    private Long id;
    private String unitId;
    private Long donationId;
    private Long donorId;
    private String donorName;
    private Long bloodBankId;
    private String bloodBankName;
    private BloodInventory.BloodType bloodType;
    private BloodUnit.BloodComponentType componentType;
    private String componentDisplayName;
    private Integer quantity;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private BloodUnit.BloodUnitStatus status;
    private boolean expired;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BloodUnitResponse() {}

    public static BloodUnitResponse fromEntity(BloodUnit unit) {
        BloodUnitResponse res = new BloodUnitResponse();
        res.setId(unit.getId());
        res.setUnitId(unit.getUnitId());
        if (unit.getDonation() != null) {
            res.setDonationId(unit.getDonation().getId());
            if (unit.getDonation().getDonor() != null) {
                res.setDonorId(unit.getDonation().getDonor().getId());
                res.setDonorName(unit.getDonation().getDonor().getFullName());
            }
        }
        if (unit.getBloodBank() != null) {
            res.setBloodBankId(unit.getBloodBank().getId());
            res.setBloodBankName(unit.getBloodBank().getName());
        }
        res.setBloodType(unit.getBloodType());
        res.setComponentType(unit.getComponentType());
        if (unit.getComponentType() != null) {
            res.setComponentDisplayName(unit.getComponentType().getDisplayName());
        }
        res.setQuantity(unit.getQuantity());
        res.setCollectionDate(unit.getCollectionDate());
        res.setExpiryDate(unit.getExpiryDate());
        res.setStatus(unit.getStatus());
        res.setExpired(unit.getExpiryDate() != null && unit.getExpiryDate().isBefore(LocalDate.now()));
        res.setNotes(unit.getNotes());
        res.setCreatedAt(unit.getCreatedAt());
        res.setUpdatedAt(unit.getUpdatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public Long getDonationId() { return donationId; }
    public void setDonationId(Long donationId) { this.donationId = donationId; }

    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }

    public String getBloodBankName() { return bloodBankName; }
    public void setBloodBankName(String bloodBankName) { this.bloodBankName = bloodBankName; }

    public BloodInventory.BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodInventory.BloodType bloodType) { this.bloodType = bloodType; }

    public BloodUnit.BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodUnit.BloodComponentType componentType) { this.componentType = componentType; }

    public String getComponentDisplayName() { return componentDisplayName; }
    public void setComponentDisplayName(String componentDisplayName) { this.componentDisplayName = componentDisplayName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public BloodUnit.BloodUnitStatus getStatus() { return status; }
    public void setStatus(BloodUnit.BloodUnitStatus status) { this.status = status; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
