package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.Donation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DonationResponse {

    private Long id;
    private Long donorId;
    private String donorName;
    private String donorPhone;
    private Long bloodBankId;
    private String bloodBankName;
    private LocalDate donationDate;
    private BloodInventory.BloodType bloodType;
    private Integer quantity;
    private String location;
    private Donation.DonationStatus status;
    private String notes;
    private LocalDateTime createdAt;

    public DonationResponse() {}

    public static DonationResponse fromEntity(Donation donation) {
        DonationResponse res = new DonationResponse();
        res.setId(donation.getId());
        if (donation.getDonor() != null) {
            res.setDonorId(donation.getDonor().getId());
            res.setDonorName(donation.getDonor().getFullName());
            res.setDonorPhone(donation.getDonor().getPhone());
        }
        if (donation.getBloodBank() != null) {
            res.setBloodBankId(donation.getBloodBank().getId());
            res.setBloodBankName(donation.getBloodBank().getName());
        }
        res.setDonationDate(donation.getDonationDate());
        res.setBloodType(donation.getBloodType());
        res.setQuantity(donation.getQuantity());
        res.setLocation(donation.getLocation());
        res.setStatus(donation.getStatus());
        res.setNotes(donation.getNotes());
        res.setCreatedAt(donation.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getDonorPhone() { return donorPhone; }
    public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }

    public String getBloodBankName() { return bloodBankName; }
    public void setBloodBankName(String bloodBankName) { this.bloodBankName = bloodBankName; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
