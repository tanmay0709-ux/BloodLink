package com.bloodlink.dto;

import jakarta.validation.constraints.Size;

public class ConfirmReceiptDto {

    @Size(max = 500, message = "Receipt notes cannot exceed 500 characters")
    private String receiptNotes;

    public ConfirmReceiptDto() {}

    public String getReceiptNotes() { return receiptNotes; }
    public void setReceiptNotes(String receiptNotes) { this.receiptNotes = receiptNotes; }
}
