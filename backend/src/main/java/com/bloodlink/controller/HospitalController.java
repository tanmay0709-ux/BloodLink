package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.HospitalRequest;
import com.bloodlink.dto.UserSummaryDto;
import com.bloodlink.model.Hospital;
import com.bloodlink.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Hospital>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(hospitalService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Hospital>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(hospitalService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Hospital>> create(@Valid @RequestBody HospitalRequest request) {
        Hospital created = hospitalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Hospital created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'HOSPITAL_STAFF')")
    public ResponseEntity<ApiResponse<Hospital>> update(
            @PathVariable Long id, @Valid @RequestBody HospitalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Hospital updated", hospitalService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        hospitalService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Hospital deactivated", null));
    }

    @GetMapping("/{id}/staff")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getStaff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(hospitalService.getStaff(id)));
    }

    @GetMapping("/{id}/pending-staff")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getPendingStaff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(hospitalService.getPendingStaff(id)));
    }

    @PostMapping("/{id}/approve-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UserSummaryDto>> approveStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Staff association approved", hospitalService.approveStaff(id, userId)));
    }

    @PostMapping("/{id}/reject-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        hospitalService.rejectStaff(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Staff association rejected", null));
    }

    @PostMapping("/{id}/assign-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UserSummaryDto>> assignStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Staff assigned to hospital", hospitalService.assignStaff(id, userId)));
    }

    @DeleteMapping("/{id}/unassign-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unassignStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        hospitalService.unassignStaff(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Staff unassigned from hospital", null));
    }
}
