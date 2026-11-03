package com.bloodlink.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(name = "blood_banks")
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Blood bank name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Size(max = 300, message = "Address must be at most 300 characters")
    @Column(length = 300)
    private String address;

    @Size(max = 20, message = "Contact phone must be at most 20 characters")
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }
}
