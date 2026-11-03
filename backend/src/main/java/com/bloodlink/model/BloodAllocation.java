package com.bloodlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blood_allocations")
public class BloodAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Request reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodRequest request;

    @NotNull(message = "Blood unit reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blood_unit_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodUnit bloodUnit;

    @NotNull(message = "Blood bank reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodBank bloodBank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "allocated_by_user_id")
    @JsonIgnoreProperties({"password", "assignedHospital", "assignedBloodBank", "hibernateLazyInitializer", "handler"})
    private User allocatedBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationStatus status;

    @Column(name = "allocated_at", nullable = false, updatable = false)
    private LocalDateTime allocatedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.allocatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AllocationStatus.ALLOCATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AllocationStatus {
        ALLOCATED,
        DISPATCHED,
        COMPLETED,
        CANCELLED
    }
}
