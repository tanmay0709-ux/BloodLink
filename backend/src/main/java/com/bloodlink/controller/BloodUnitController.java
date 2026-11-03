package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.BloodUnitRequest;
import com.bloodlink.dto.BloodUnitResponse;
import com.bloodlink.dto.StockSummaryDto;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodUnit;
import com.bloodlink.service.BloodUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-units")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
public class BloodUnitController {

    private final BloodUnitService bloodUnitService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BloodUnitResponse>>> getAll(
            @RequestParam(required = false) BloodInventory.BloodType bloodType,
            @RequestParam(required = false) BloodUnit.BloodComponentType componentType,
            @RequestParam(required = false) BloodUnit.BloodUnitStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long bloodBankId) {
        return ResponseEntity.ok(ApiResponse.ok(
            bloodUnitService.getAll(bloodType, componentType, status, q, bloodBankId)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodUnitResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bloodUnitService.getById(id)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StockSummaryDto>> getStockSummary(
            @RequestParam(required = false) Long bloodBankId) {
        return ResponseEntity.ok(ApiResponse.ok(bloodUnitService.getStockSummary(bloodBankId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BloodUnitResponse>> create(
            @Valid @RequestBody BloodUnitRequest request) {
        BloodUnitResponse created = bloodUnitService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Blood unit registered successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BloodUnitResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BloodUnitRequest request) {
        BloodUnitResponse updated = bloodUnitService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Blood unit updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BloodUnitResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam BloodUnit.BloodUnitStatus status) {
        BloodUnitResponse updated = bloodUnitService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Blood unit status updated", updated));
    }

    @PostMapping("/check-expired")
    public ResponseEntity<ApiResponse<String>> checkExpired() {
        bloodUnitService.checkAndMarkExpiredUnits();
        return ResponseEntity.ok(ApiResponse.ok("Expired units check completed", "OK"));
    }
}
