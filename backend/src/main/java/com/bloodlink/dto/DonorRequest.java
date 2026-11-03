package com.bloodlink.dto;

import com.bloodlink.model.BloodInventory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class DonorRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @NotNull(message = "Blood group is required")
    private BloodInventory.BloodType bloodType;

    @Size(max = 10)
    private String gender;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @Size(max = 255)
    private String address;

    private Long bloodBankId; // Optional for admin, automatic for staff

    public DonorRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BloodInventory.BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodInventory.BloodType bloodType) { this.bloodType = bloodType; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }
}
