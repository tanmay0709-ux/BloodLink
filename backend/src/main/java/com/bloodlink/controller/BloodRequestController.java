package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.BloodRequestCreateDto;
import com.bloodlink.dto.BloodRequestResponse;
import com.bloodlink.dto.MatchingInventoryDto;
import com.bloodlink.model.BloodRequest;
import com.bloodlink.service.BloodRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> create(
            @Valid @RequestBody BloodRequestCreateDto dto) {
        BloodRequestResponse created = bloodRequestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Blood request submitted successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BloodRequestResponse>>> getAll(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) BloodRequest.RequestStatus status,
            @RequestParam(required = false) BloodRequest.RequestPriority priority) {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodRequestService.getAllRequests(hospitalId, status, priority)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodRequestService.getRequestById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BloodRequestResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam BloodRequest.RequestStatus status) {
        BloodRequestResponse updated = bloodRequestService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Request status updated", updated));
    }

    @GetMapping("/{id}/matching-units")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<MatchingInventoryDto>> getMatchingUnits(
            @PathVariable Long id,
            @RequestParam(required = false) Long bloodBankId) {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodRequestService.getMatchingInventory(id, bloodBankId)
        ));
    }
}
