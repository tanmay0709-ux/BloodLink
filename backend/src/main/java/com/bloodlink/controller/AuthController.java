package com.bloodlink.controller;

import com.bloodlink.dto.ApiResponse;
import com.bloodlink.dto.JwtResponse;
import com.bloodlink.dto.LoginRequest;
import com.bloodlink.dto.RegisterRequest;
import com.bloodlink.model.BloodBank;
import com.bloodlink.model.Hospital;
import com.bloodlink.model.User;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.HospitalRepository;
import com.bloodlink.repository.UserRepository;
import com.bloodlink.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;

    /**
     * GET /api/auth/hospitals
     * Public endpoint to list active hospitals for registration dropdown.
     */
    @GetMapping("/hospitals")
    public ResponseEntity<ApiResponse<List<Hospital>>> getActiveHospitals() {
        return ResponseEntity.ok(ApiResponse.ok(hospitalRepository.findAllByActiveTrue()));
    }

    /**
     * GET /api/auth/blood-banks
     * Public endpoint to list active blood banks for registration dropdown.
     */
    @GetMapping("/blood-banks")
    public ResponseEntity<ApiResponse<List<BloodBank>>> getActiveBloodBanks() {
        return ResponseEntity.ok(ApiResponse.ok(bloodBankRepository.findAllByActiveTrue()));
    }

    /**
     * POST /api/auth/register
     * Register a new user. Returns a JWT token on success.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest request) {
        JwtResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    /**
     * POST /api/auth/login
     * Authenticate a user. Returns a JWT token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    /**
     * GET /api/auth/me
     * Returns the currently authenticated user's profile.
     * Requires a valid JWT (protected). Uses Authentication directly
     * to avoid @PreAuthorize + @AuthenticationPrincipal AOP conflict.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB: " + username));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());
        profile.put("assignedHospitalId", user.getAssignedHospital() != null ? user.getAssignedHospital().getId() : null);
        profile.put("assignedHospitalName", user.getAssignedHospital() != null ? user.getAssignedHospital().getName() : null);
        profile.put("pendingHospitalId", user.getPendingHospital() != null ? user.getPendingHospital().getId() : null);
        profile.put("pendingHospitalName", user.getPendingHospital() != null ? user.getPendingHospital().getName() : null);
        profile.put("assignedBloodBankId", user.getAssignedBloodBank() != null ? user.getAssignedBloodBank().getId() : null);
        profile.put("assignedBloodBankName", user.getAssignedBloodBank() != null ? user.getAssignedBloodBank().getName() : null);
        profile.put("pendingBloodBankId", user.getPendingBloodBank() != null ? user.getPendingBloodBank().getId() : null);
        profile.put("pendingBloodBankName", user.getPendingBloodBank() != null ? user.getPendingBloodBank().getName() : null);
        profile.put("approvalStatus", user.getApprovalStatus() != null ? user.getApprovalStatus().name()
            : ((user.getAssignedHospital() != null || user.getAssignedBloodBank() != null) ? "APPROVED" : null));

        return ResponseEntity.ok(ApiResponse.ok("Authenticated", profile));
    }

    /**
     * GET /api/auth/health
     * Public health check — no token needed.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Auth service is running", "OK"));
    }
}
