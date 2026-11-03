package com.bloodlink.dto;

public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;
    private String approvalStatus;
    private Long assignedHospitalId;
    private String assignedHospitalName;
    private Long pendingHospitalId;
    private String pendingHospitalName;
    private Long assignedBloodBankId;
    private String assignedBloodBankName;
    private Long pendingBloodBankId;
    private String pendingBloodBankName;

    public JwtResponse() {}

    public JwtResponse(String token, String username, String email, String role) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public JwtResponse(String token, Long id, String username, String email, String role,
                       String approvalStatus, Long assignedHospitalId, String assignedHospitalName,
                       Long pendingHospitalId, String pendingHospitalName) {
        this(token, id, username, email, role, approvalStatus,
             assignedHospitalId, assignedHospitalName, pendingHospitalId, pendingHospitalName,
             null, null, null, null);
    }

    public JwtResponse(String token, Long id, String username, String email, String role,
                       String approvalStatus, Long assignedHospitalId, String assignedHospitalName,
                       Long pendingHospitalId, String pendingHospitalName,
                       Long assignedBloodBankId, String assignedBloodBankName,
                       Long pendingBloodBankId, String pendingBloodBankName) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.approvalStatus = approvalStatus;
        this.assignedHospitalId = assignedHospitalId;
        this.assignedHospitalName = assignedHospitalName;
        this.pendingHospitalId = pendingHospitalId;
        this.pendingHospitalName = pendingHospitalName;
        this.assignedBloodBankId = assignedBloodBankId;
        this.assignedBloodBankName = assignedBloodBankName;
        this.pendingBloodBankId = pendingBloodBankId;
        this.pendingBloodBankName = pendingBloodBankName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public Long getAssignedHospitalId() { return assignedHospitalId; }
    public void setAssignedHospitalId(Long assignedHospitalId) { this.assignedHospitalId = assignedHospitalId; }

    public String getAssignedHospitalName() { return assignedHospitalName; }
    public void setAssignedHospitalName(String assignedHospitalName) { this.assignedHospitalName = assignedHospitalName; }

    public Long getPendingHospitalId() { return pendingHospitalId; }
    public void setPendingHospitalId(Long pendingHospitalId) { this.pendingHospitalId = pendingHospitalId; }

    public String getPendingHospitalName() { return pendingHospitalName; }
    public void setPendingHospitalName(String pendingHospitalName) { this.pendingHospitalName = pendingHospitalName; }

    public Long getAssignedBloodBankId() { return assignedBloodBankId; }
    public void setAssignedBloodBankId(Long assignedBloodBankId) { this.assignedBloodBankId = assignedBloodBankId; }

    public String getAssignedBloodBankName() { return assignedBloodBankName; }
    public void setAssignedBloodBankName(String assignedBloodBankName) { this.assignedBloodBankName = assignedBloodBankName; }

    public Long getPendingBloodBankId() { return pendingBloodBankId; }
    public void setPendingBloodBankId(Long pendingBloodBankId) { this.pendingBloodBankId = pendingBloodBankId; }

    public String getPendingBloodBankName() { return pendingBloodBankName; }
    public void setPendingBloodBankName(String pendingBloodBankName) { this.pendingBloodBankName = pendingBloodBankName; }
}
