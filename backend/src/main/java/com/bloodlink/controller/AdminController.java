package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.UserSummaryDto;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    /**
     * GET /api/admin/users
     * List all users — SYSTEM_ADMIN only.
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> listUsers() {
        List<UserSummaryDto> users = userRepository.findAll().stream()
            .map(u -> {
                Long hId = u.getAssignedHospital() != null ? u.getAssignedHospital().getId() : null;
                String hName = u.getAssignedHospital() != null ? u.getAssignedHospital().getName() : null;
                Long pHId = u.getPendingHospital() != null ? u.getPendingHospital().getId() : null;
                String pHName = u.getPendingHospital() != null ? u.getPendingHospital().getName() : null;
                String approvalStatus = u.getApprovalStatus() != null ? u.getApprovalStatus().name()
                    : ((u.getAssignedHospital() != null || u.getAssignedBloodBank() != null) ? "APPROVED" : null);
                Long bId = u.getAssignedBloodBank() != null ? u.getAssignedBloodBank().getId() : null;
                String bName = u.getAssignedBloodBank() != null ? u.getAssignedBloodBank().getName() : null;
                Long pBId = u.getPendingBloodBank() != null ? u.getPendingBloodBank().getId() : null;
                String pBName = u.getPendingBloodBank() != null ? u.getPendingBloodBank().getName() : null;
                return new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail(),
                    u.getRole().name(), hId, hName, pHId, pHName, approvalStatus, bId, bName, pBId, pBName);
            })
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}
