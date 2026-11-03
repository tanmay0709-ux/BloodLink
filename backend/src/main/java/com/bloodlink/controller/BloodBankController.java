package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.BloodBankRequest;
import com.bloodlink.dto.UserSummaryDto;
import com.bloodlink.model.BloodBank;
import com.bloodlink.service.BloodBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-banks")
@RequiredArgsConstructor
public class BloodBankController {

    private final BloodBankService bloodBankService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BloodBank>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(bloodBankService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BloodBank>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodBankService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<BloodBank>> create(@Valid @RequestBody BloodBankRequest request) {
        BloodBank created = bloodBankService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Blood bank created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'BLOOD_BANK_STAFF')")
    public ResponseEntity<ApiResponse<BloodBank>> update(
            @PathVariable Long id, @Valid @RequestBody BloodBankRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Blood bank updated", bloodBankService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bloodBankService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Blood bank deactivated", null));
    }

    @GetMapping("/{id}/staff")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getStaff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodBankService.getStaff(id)));
    }

    @GetMapping("/{id}/pending-staff")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getPendingStaff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodBankService.getPendingStaff(id)));
    }

    @PostMapping("/{id}/approve-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UserSummaryDto>> approveStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Staff association approved", bloodBankService.approveStaff(id, userId)));
    }

    @PostMapping("/{id}/reject-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        bloodBankService.rejectStaff(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Staff association rejected", null));
    }

    @PostMapping("/{id}/assign-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UserSummaryDto>> assignStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Staff assigned to blood bank", bloodBankService.assignStaff(id, userId)));
    }

    @DeleteMapping("/{id}/unassign-staff/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unassignStaff(
            @PathVariable Long id, @PathVariable Long userId) {
        bloodBankService.unassignStaff(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Staff unassigned from blood bank", null));
    }
}
