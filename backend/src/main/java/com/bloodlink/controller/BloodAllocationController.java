package com.bloodlink.controller;

import com.bloodlink.dto.*;
import com.bloodlink.service.BloodAllocationService;
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
public class BloodAllocationController {

    private final BloodAllocationService bloodAllocationService;

    /**
     * POST /api/blood-requests/{id}/allocate
     * Only BLOOD_BANK_STAFF and SYSTEM_ADMIN can allocate blood units to REVIEWED requests.
     */
    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<AllocationResponseDto>>> allocateUnits(
            @PathVariable Long id,
            @Valid @RequestBody AllocationRequestDto dto) {
        List<AllocationResponseDto> allocations = bloodAllocationService.allocateUnits(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Blood units successfully allocated to request #" + id, allocations));
    }

    /**
     * GET /api/blood-requests/{id}/allocations
     * View allocations for a request.
     */
    @GetMapping("/{id}/allocations")
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<AllocationResponseDto>>> getAllocations(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodAllocationService.getAllocationsForRequest(id)
        ));
    }

    /**
     * POST /api/blood-requests/{id}/dispatch
     * Only BLOOD_BANK_STAFF and SYSTEM_ADMIN can dispatch allocated units.
     */
    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DispatchResponseDto>> dispatchRequest(
            @PathVariable Long id,
            @RequestBody(required = false) DispatchRequestDto dto) {
        DispatchResponseDto dispatch = bloodAllocationService.dispatchRequest(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Shipment dispatched successfully", dispatch));
    }

    /**
     * GET /api/blood-requests/{id}/dispatch
     * View dispatch details for a request.
     */
    @GetMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DispatchResponseDto>> getDispatchByRequestId(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodAllocationService.getDispatchByRequestId(id)
        ));
    }
}
