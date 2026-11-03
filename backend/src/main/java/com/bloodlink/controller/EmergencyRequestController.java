package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.model.EmergencyRequest;
import com.bloodlink.service.EmergencyRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class EmergencyRequestController {

    private final EmergencyRequestService emergencyRequestService;

    /** Any authenticated user can view emergency requests */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmergencyRequest>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getAllRequests()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmergencyRequest>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getRequestById(id)));
    }

    @GetMapping("/hospital/{hospital}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmergencyRequest>>> getByHospital(@PathVariable String hospital) {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getRequestsByHospital(hospital)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmergencyRequest>>> getByStatus(
            @PathVariable EmergencyRequest.RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(emergencyRequestService.getRequestsByStatus(status)));
    }

    /** HOSPITAL_STAFF and SYSTEM_ADMIN can create requests */
    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<EmergencyRequest>> create(@RequestBody EmergencyRequest request) {
        EmergencyRequest created = emergencyRequestService.createRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Emergency request created", created));
    }

    /** BLOOD_BANK_STAFF and SYSTEM_ADMIN can approve/reject/fulfil */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<EmergencyRequest>> updateStatus(
            @PathVariable Long id, @RequestParam EmergencyRequest.RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated",
            emergencyRequestService.updateStatus(id, status)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<EmergencyRequest>> update(
            @PathVariable Long id, @RequestBody EmergencyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Request updated",
            emergencyRequestService.updateRequest(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        emergencyRequestService.deleteRequest(id);
        return ResponseEntity.ok(ApiResponse.ok("Emergency request deleted", null));
    }
}
