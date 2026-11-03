package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.DonationResponse;
import com.bloodlink.dto.DonorRequest;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.Donor;
import com.bloodlink.service.DonationService;
import com.bloodlink.service.DonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BLOOD_BANK_STAFF', 'SYSTEM_ADMIN')")
public class DonorController {

    private final DonorService donorService;
    private final DonationService donationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Donor>>> getAllDonors(
            @RequestParam(required = false) Long bloodBankId) {
        return ResponseEntity.ok(ApiResponse.ok(donorService.getAllDonors(bloodBankId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Donor>> getDonorById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(donorService.getDonorById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Donor>>> searchDonors(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "bloodType", required = false) BloodInventory.BloodType bloodType,
            @RequestParam(name = "bloodBankId", required = false) Long bloodBankId) {
        return ResponseEntity.ok(ApiResponse.ok(donorService.searchDonors(query, bloodType, bloodBankId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Donor>> registerDonor(@Valid @RequestBody DonorRequest request) {
        Donor created = donorService.registerDonor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Donor registered successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Donor>> updateDonor(
            @PathVariable Long id, @Valid @RequestBody DonorRequest request) {
        Donor updated = donorService.updateDonor(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Donor updated successfully", updated));
    }

    @GetMapping("/{id}/donations")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getDonorDonations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(donationService.getDonationsByDonor(id)));
    }
}
