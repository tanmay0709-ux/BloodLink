package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.Donation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class DonationRequest {

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    private Long bloodBankId; // Optional; staff uses assigned, admin can supply

    private LocalDate donationDate; // Defaults to now if not provided

    private BloodInventory.BloodType bloodType; // Defaults to donor's blood type if not provided

    @NotNull(message = "Quantity is required")
    @Min(value = 50, message = "Quantity must be at least 50 ml")
    @Max(value = 1000, message = "Quantity cannot exceed 1000 ml")
    private Integer quantity;

    @Size(max = 150)
    private String location;

    private Donation.DonationStatus status;

    @Size(max = 500)
    private String notes;

    public DonationRequest() {}

    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }

    public LocalDate getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) { this.donationDate = donationDate; }

    public BloodInventory.BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodInventory.BloodType bloodType) { this.bloodType = bloodType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Donation.DonationStatus getStatus() { return status; }
    public void setStatus(Donation.DonationStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
