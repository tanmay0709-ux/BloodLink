package com.bloodlink.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_hospital_id")
    @JsonIgnore
    private Hospital assignedHospital;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pending_hospital_id")
    @JsonIgnore
    private Hospital pendingHospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private ApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_blood_bank_id")
    @JsonIgnore
    private BloodBank assignedBloodBank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pending_blood_bank_id")
    @JsonIgnore
    private BloodBank pendingBloodBank;

    public boolean isApprovedHospitalStaff() {
        if (role != Role.HOSPITAL_STAFF) return false;
        if (assignedHospital == null) return false;
        return approvalStatus == ApprovalStatus.APPROVED || approvalStatus == null;
    }

    public boolean isApprovedBloodBankStaff() {
        if (role != Role.BLOOD_BANK_STAFF) return false;
        if (assignedBloodBank == null) return false;
        return approvalStatus == ApprovalStatus.APPROVED || approvalStatus == null;
    }

    public enum Role {
        SYSTEM_ADMIN,
        HOSPITAL_STAFF,
        BLOOD_BANK_STAFF
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
