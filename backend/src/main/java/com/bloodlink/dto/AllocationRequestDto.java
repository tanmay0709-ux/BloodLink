package com.bloodlink.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AllocationRequestDto {

    @NotEmpty(message = "At least one blood unit must be selected for allocation")
    private List<Long> unitIds;

    private Long bloodBankId; // Optional for admin, automatic for blood bank staff

    public AllocationRequestDto() {}

    public List<Long> getUnitIds() { return unitIds; }
    public void setUnitIds(List<Long> unitIds) { this.unitIds = unitIds; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }
}
