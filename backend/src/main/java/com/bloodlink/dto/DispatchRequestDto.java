package com.bloodlink.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class DispatchRequestDto {

    private LocalDate dispatchDate;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    public DispatchRequestDto() {}

    public LocalDate getDispatchDate() { return dispatchDate; }
    public void setDispatchDate(LocalDate dispatchDate) { this.dispatchDate = dispatchDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
