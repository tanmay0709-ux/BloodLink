package com.bloodlink.dto;

import com.bloodlink.model.BloodDispatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DispatchResponseDto {

    private Long id;
    private String dispatchReference;
    private Long requestId;
    private Long hospitalId;
    private String hospitalName;
    private Long bloodBankId;
    private String bloodBankName;
    private LocalDate dispatchDate;
    private LocalDateTime dispatchedAt;
    private String dispatchedByUsername;
    private BloodDispatch.DispatchStatus status;
    private String notes;
    private LocalDateTime receivedAt;
    private String receivedByUsername;
    private String receiptNotes;
    private List<BloodUnitResponse> units;

    public DispatchResponseDto() {}

    public static DispatchResponseDto fromEntity(BloodDispatch d) {
        DispatchResponseDto dto = new DispatchResponseDto();
        dto.setId(d.getId());
        dto.setDispatchReference(d.getDispatchReference());
        if (d.getRequest() != null) {
            dto.setRequestId(d.getRequest().getId());
        }
        if (d.getHospital() != null) {
            dto.setHospitalId(d.getHospital().getId());
            dto.setHospitalName(d.getHospital().getName());
        }
        if (d.getBloodBank() != null) {
            dto.setBloodBankId(d.getBloodBank().getId());
            dto.setBloodBankName(d.getBloodBank().getName());
        }
        dto.setDispatchDate(d.getDispatchDate());
        dto.setDispatchedAt(d.getDispatchedAt());
        if (d.getDispatchedBy() != null) {
            dto.setDispatchedByUsername(d.getDispatchedBy().getUsername());
        }
        dto.setStatus(d.getStatus());
        dto.setNotes(d.getNotes());
        dto.setReceivedAt(d.getReceivedAt());
        if (d.getReceivedBy() != null) {
            dto.setReceivedByUsername(d.getReceivedBy().getUsername());
        }
        dto.setReceiptNotes(d.getReceiptNotes());
        if (d.getUnits() != null) {
            dto.setUnits(d.getUnits().stream().map(BloodUnitResponse::fromEntity).toList());
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDispatchReference() { return dispatchReference; }
    public void setDispatchReference(String dispatchReference) { this.dispatchReference = dispatchReference; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public Long getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(Long bloodBankId) { this.bloodBankId = bloodBankId; }

    public String getBloodBankName() { return bloodBankName; }
    public void setBloodBankName(String bloodBankName) { this.bloodBankName = bloodBankName; }

    public LocalDate getDispatchDate() { return dispatchDate; }
    public void setDispatchDate(LocalDate dispatchDate) { this.dispatchDate = dispatchDate; }

    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public String getDispatchedByUsername() { return dispatchedByUsername; }
    public void setDispatchedByUsername(String dispatchedByUsername) { this.dispatchedByUsername = dispatchedByUsername; }

    public BloodDispatch.DispatchStatus getStatus() { return status; }
    public void setStatus(BloodDispatch.DispatchStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getReceivedByUsername() { return receivedByUsername; }
    public void setReceivedByUsername(String receivedByUsername) { this.receivedByUsername = receivedByUsername; }

    public String getReceiptNotes() { return receiptNotes; }
    public void setReceiptNotes(String receiptNotes) { this.receiptNotes = receiptNotes; }

    public List<BloodUnitResponse> getUnits() { return units; }
    public void setUnits(List<BloodUnitResponse> units) { this.units = units; }
}
