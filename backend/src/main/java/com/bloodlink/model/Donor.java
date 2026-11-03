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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donors")
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Size(max = 100, message = "Email must be at most 100 characters")
    @Column(unique = true, length = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 20)
    private BloodInventory.BloodType bloodType;

    @Size(max = 10)
    @Column(length = 10)
    private String gender; // MALE, FEMALE, OTHER

    @NotNull(message = "Date of birth is required")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BloodBank bloodBank;

    @Builder.Default
    @Column(nullable = false)
    private Boolean eligible = true;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.eligible == null) {
            this.eligible = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
