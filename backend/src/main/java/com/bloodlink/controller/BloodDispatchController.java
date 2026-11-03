package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.ConfirmReceiptDto;
import com.bloodlink.dto.DispatchResponseDto;
import com.bloodlink.service.BloodAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatches")
@RequiredArgsConstructor
public class BloodDispatchController {

    private final BloodAllocationService bloodAllocationService;

    /**
     * GET /api/dispatches
     * List all dispatches accessible to the current user (hospital staff sees hospital dispatches, bank staff sees bank dispatches, admin sees all).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<DispatchResponseDto>>> getAllDispatches() {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodAllocationService.getAllDispatches()
        ));
    }

    /**
     * GET /api/dispatches/{id}
     * Get single dispatch details.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DispatchResponseDto>> getDispatchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodAllocationService.getDispatchById(id)
        ));
    }

    /**
     * POST /api/dispatches/{id}/confirm-receipt
     * Only HOSPITAL_STAFF assigned to the requesting hospital (or SYSTEM_ADMIN) can confirm receipt.
     */
    @PostMapping("/{id}/confirm-receipt")
    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DispatchResponseDto>> confirmReceipt(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmReceiptDto dto) {
        DispatchResponseDto updated = bloodAllocationService.confirmReceipt(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Delivery and receipt successfully confirmed", updated));
    }
}
