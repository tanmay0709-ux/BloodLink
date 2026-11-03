package com.bloodlink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String role; // Optional: default to VIEWER if not provided

    private Long hospitalId; // Required when role is HOSPITAL_STAFF

    private Long bloodBankId; // Required when role is BLOOD_BANK_STAFF

    public RegisterRequest() {}

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public RegisterRequest(String username, String email, String password, String role, Long hospitalId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.hospitalId = hospitalId;
    }

    public RegisterRequest(String username, String email, String password, String role, Long hospitalId, Long bloodBankId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.hospitalId = hospitalId;
        this.bloodBankId = bloodBankId;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }
}
