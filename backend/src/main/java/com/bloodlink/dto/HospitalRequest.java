package com.bloodlink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class HospitalRequest {

    @NotBlank(message = "Hospital name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 300)
    private String address;

    @Size(max = 20)
    private String contactPhone;

    public HospitalRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}
