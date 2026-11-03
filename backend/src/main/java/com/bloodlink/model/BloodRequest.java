package com.bloodlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Hospital is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Hospital hospital;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 20)
    private BloodInventory.BloodType bloodType;

    @NotNull(message = "Component type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private BloodUnit.BloodComponentType componentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1 unit")
    @Column(nullable = false)
    private Integer quantity; // number of units requested

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestPriority priority;

    @NotNull(message = "Required date is required")
    @Column(name = "required_date", nullable = false)
    private LocalDate requiredDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnoreProperties({"password", "assignedHospital", "assignedBloodBank", "hibernateLazyInitializer", "handler"})
    private User createdBy;

    @Size(max = 500)
    @Column(name = "clinical_reason", length = 500)
    private String clinicalReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RequestStatus.PENDING;
        }
        if (this.priority == null) {
            this.priority = RequestPriority.ROUTINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RequestPriority {
        ROUTINE,
        URGENT,
        EMERGENCY
    }

    public enum RequestStatus {
        PENDING,
        REVIEWED,
        ALLOCATED,
        DISPATCHED,
        FULFILLED,
        CANCELLED,
        REJECTED
    }
}
