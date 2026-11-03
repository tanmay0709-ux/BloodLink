package com.bloodlink.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "blood_units")
public class BloodUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Unit ID is required")
    @Size(max = 50, message = "Unit ID must be at most 50 characters")
    @Column(name = "unit_id", nullable = false, unique = true, length = 50)
    private String unitId;

    @NotNull(message = "Donation reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donation_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Donation donation;

    @NotNull(message = "Blood bank reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodBank bloodBank;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 20)
    private BloodInventory.BloodType bloodType;

    @NotNull(message = "Component type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private BloodComponentType componentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    @Column(nullable = false)
    private Integer quantity; // in ml

    @NotNull(message = "Collection date is required")
    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BloodUnitStatus status;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BloodUnitStatus.AVAILABLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum BloodComponentType {
        WHOLE_BLOOD("Whole Blood", 35),
        RED_BLOOD_CELLS("Packed Red Blood Cells (PRBC)", 42),
        PLATELETS("Platelet Concentrate", 5),
        PLASMA("Fresh Frozen Plasma (FFP)", 365),
        CRYOPRECIPITATE("Cryoprecipitate", 365);

        private final String displayName;
        private final int standardShelfLifeDays;

        BloodComponentType(String displayName, int standardShelfLifeDays) {
            this.displayName = displayName;
            this.standardShelfLifeDays = standardShelfLifeDays;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getStandardShelfLifeDays() {
            return standardShelfLifeDays;
        }
    }

    public enum BloodUnitStatus {
        AVAILABLE,
        RESERVED,
        ISSUED,
        EXPIRED,
        DISCARDED,
        QUARANTINED
    }
}
