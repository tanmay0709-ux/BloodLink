package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.DonationRequest;
import com.bloodlink.dto.DonationResponse;
import com.bloodlink.model.Donation;
import com.bloodlink.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
public class DonationController {

    private final DonationService donationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getAllDonations(
            @RequestParam(required = false) Long bloodBankId) {
        return ResponseEntity.ok(ApiResponse.ok(donationService.getAllDonations(bloodBankId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DonationResponse>> getDonationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(donationService.getDonationById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DonationResponse>> recordDonation(
            @Valid @RequestBody DonationRequest request) {
        DonationResponse created = donationService.recordDonation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Donation recorded successfully", created));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DonationResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam Donation.DonationStatus status) {
        DonationResponse updated = donationService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Donation status updated", updated));
    }
}
