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
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Donor is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Donor donor;

    @NotNull(message = "Blood bank is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodBank bloodBank;

    @NotNull(message = "Donation date is required")
    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 20)
    private BloodInventory.BloodType bloodType;

    @NotNull(message = "Quantity is required")
    @Min(value = 50, message = "Quantity must be at least 50 ml")
    @Column(nullable = false)
    private Integer quantity; // in ml

    @Size(max = 150)
    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DonationStatus.COMPLETED;
        }
        if (this.donationDate == null) {
            this.donationDate = LocalDate.now();
        }
    }

    public enum DonationStatus {
        COMPLETED,
        PENDING,
        REJECTED
    }
}
