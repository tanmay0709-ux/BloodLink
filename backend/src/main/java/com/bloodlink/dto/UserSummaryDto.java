package com.bloodlink.dto;

public class UserSummaryDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Long assignedHospitalId;
    private String assignedHospitalName;
    private Long pendingHospitalId;
    private String pendingHospitalName;
    private String approvalStatus;
    private Long assignedBloodBankId;
    private String assignedBloodBankName;
    private Long pendingBloodBankId;
    private String pendingBloodBankName;

    public UserSummaryDto() {}

    public UserSummaryDto(Long id, String username, String email, String role,
                          Long assignedHospitalId, String assignedHospitalName,
                          Long assignedBloodBankId, String assignedBloodBankName) {
        this(id, username, email, role, assignedHospitalId, assignedHospitalName,
             null, null, null, assignedBloodBankId, assignedBloodBankName, null, null);
    }

    public UserSummaryDto(Long id, String username, String email, String role,
                          Long assignedHospitalId, String assignedHospitalName,
                          Long pendingHospitalId, String pendingHospitalName,
                          String approvalStatus,
                          Long assignedBloodBankId, String assignedBloodBankName) {
        this(id, username, email, role, assignedHospitalId, assignedHospitalName,
             pendingHospitalId, pendingHospitalName, approvalStatus,
             assignedBloodBankId, assignedBloodBankName, null, null);
    }

    public UserSummaryDto(Long id, String username, String email, String role,
                          Long assignedHospitalId, String assignedHospitalName,
                          Long pendingHospitalId, String pendingHospitalName,
                          String approvalStatus,
                          Long assignedBloodBankId, String assignedBloodBankName,
                          Long pendingBloodBankId, String pendingBloodBankName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.assignedHospitalId = assignedHospitalId;
        this.assignedHospitalName = assignedHospitalName;
        this.pendingHospitalId = pendingHospitalId;
        this.pendingHospitalName = pendingHospitalName;
        this.approvalStatus = approvalStatus;
        this.assignedBloodBankId = assignedBloodBankId;
        this.assignedBloodBankName = assignedBloodBankName;
        this.pendingBloodBankId = pendingBloodBankId;
        this.pendingBloodBankName = pendingBloodBankName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getAssignedHospitalId() { return assignedHospitalId; }
    public void setAssignedHospitalId(Long assignedHospitalId) { this.assignedHospitalId = assignedHospitalId; }
    public String getAssignedHospitalName() { return assignedHospitalName; }
    public void setAssignedHospitalName(String assignedHospitalName) { this.assignedHospitalName = assignedHospitalName; }
    public Long getPendingHospitalId() { return pendingHospitalId; }
    public void setPendingHospitalId(Long pendingHospitalId) { this.pendingHospitalId = pendingHospitalId; }
    public String getPendingHospitalName() { return pendingHospitalName; }
    public void setPendingHospitalName(String pendingHospitalName) { this.pendingHospitalName = pendingHospitalName; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public Long getAssignedBloodBankId() { return assignedBloodBankId; }
    public void setAssignedBloodBankId(Long assignedBloodBankId) { this.assignedBloodBankId = assignedBloodBankId; }
    public String getAssignedBloodBankName() { return assignedBloodBankName; }
    public void setAssignedBloodBankName(String assignedBloodBankName) { this.assignedBloodBankName = assignedBloodBankName; }
    public Long getPendingBloodBankId() { return pendingBloodBankId; }
    public void setPendingBloodBankId(Long pendingBloodBankId) { this.pendingBloodBankId = pendingBloodBankId; }
    public String getPendingBloodBankName() { return pendingBloodBankName; }
    public void setPendingBloodBankName(String pendingBloodBankName) { this.pendingBloodBankName = pendingBloodBankName; }
}
