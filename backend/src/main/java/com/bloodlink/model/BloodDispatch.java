package com.bloodlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blood_dispatches")
public class BloodDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Dispatch reference is required")
    @Column(name = "dispatch_reference", nullable = false, unique = true, length = 50)
    private String dispatchReference;

    @NotNull(message = "Request reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodRequest request;

    @NotNull(message = "Blood bank is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodBank bloodBank;

    @NotNull(message = "Hospital is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dispatched_by_user_id")
    @JsonIgnoreProperties({"password", "assignedHospital", "assignedBloodBank", "hibernateLazyInitializer", "handler"})
    private User dispatchedBy;

    @NotNull(message = "Dispatch date is required")
    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "dispatched_at", nullable = false, updatable = false)
    private LocalDateTime dispatchedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispatchStatus status;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "received_by_user_id")
    @JsonIgnoreProperties({"password", "assignedHospital", "assignedBloodBank", "hibernateLazyInitializer", "handler"})
    private User receivedBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Size(max = 500)
    @Column(name = "receipt_notes", length = 500)
    private String receiptNotes;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "blood_dispatch_units",
        joinColumns = @JoinColumn(name = "dispatch_id"),
        inverseJoinColumns = @JoinColumn(name = "blood_unit_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<BloodUnit> units = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.dispatchedAt = LocalDateTime.now();
        if (this.dispatchDate == null) {
            this.dispatchDate = LocalDate.now();
        }
        if (this.status == null) {
            this.status = DispatchStatus.DISPATCHED;
        }
    }

    public enum DispatchStatus {
        DISPATCHED,
        DELIVERED,
        CANCELLED
    }
}
