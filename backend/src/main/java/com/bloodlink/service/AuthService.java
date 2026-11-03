
package com.bloodlink.service;

import com.bloodlink.dto.JwtResponse;
import com.bloodlink.dto.LoginRequest;
import com.bloodlink.dto.RegisterRequest;
import com.bloodlink.model.BloodBank;
import com.bloodlink.model.Hospital;
import com.bloodlink.model.User;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.HospitalRepository;
import com.bloodlink.repository.UserRepository;
import com.bloodlink.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger =
        LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Authenticate a user and return a JWT response.
     */
    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {

        logger.info("========== LOGIN ATTEMPT ==========");
        logger.info("Username received: [{}]", request.getUsername());
        logger.info("Password length: {}", request.getPassword().length());

        try {
            Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                    )
                );

            logger.info("AUTHENTICATION SUCCESS: {}",
                authentication.isAuthenticated());

            SecurityContextHolder.getContext()
                .setAuthentication(authentication);

            UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

            String token =
                jwtTokenProvider.generateToken(userDetails);

            User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                    new RuntimeException(
                        "User not found after authentication"
                    )
                );

            logger.info("LOGIN COMPLETED FOR: {}", user.getUsername());
            logger.info("===================================");

            return buildJwtResponse(token, user);

        } catch (Exception e) {
            logger.error("LOGIN FAILED: {}", e.getClass().getSimpleName());
            logger.error("Failure message: {}", e.getMessage());
            logger.info("===================================");
            throw e;
        }
    }

    /**
     * Register a new user account.
     */
    @Transactional
    public JwtResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                "Username '" + request.getUsername() +
                "' is already taken"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "Email '" + request.getEmail() +
                "' is already registered"
            );
        }

        // Only HOSPITAL_STAFF and BLOOD_BANK_STAFF
        // are allowed through public registration.
        User.Role role = User.Role.HOSPITAL_STAFF;

        if (request.getRole() != null &&
            !request.getRole().isBlank()) {

            String requestedRoleStr =
                request.getRole().toUpperCase();

            if (requestedRoleStr.equals("SYSTEM_ADMIN")) {
                throw new IllegalArgumentException(
                    "SYSTEM_ADMIN accounts cannot be created via public registration."
                );
            }

            try {
                role = User.Role.valueOf(requestedRoleStr);
            } catch (IllegalArgumentException ignored) {
                // Default to HOSPITAL_STAFF.
            }
        }

        Hospital pendingHospital = null;
        BloodBank pendingBloodBank = null;

        User.ApprovalStatus approvalStatus =
            User.ApprovalStatus.APPROVED;

        if (role == User.Role.HOSPITAL_STAFF) {

            if (request.getHospitalId() == null) {
                throw new IllegalArgumentException(
                    "Hospital selection is required for hospital staff registration"
                );
            }

            pendingHospital = hospitalRepository
                .findById(request.getHospitalId())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Hospital not found with id: " +
                        request.getHospitalId()
                    )
                );

            if (!pendingHospital.getActive()) {
                throw new IllegalArgumentException(
                    "Selected hospital is inactive"
                );
            }

            approvalStatus = User.ApprovalStatus.PENDING;

        } else if (role == User.Role.BLOOD_BANK_STAFF) {

            if (request.getBloodBankId() == null) {
                throw new IllegalArgumentException(
                    "Blood bank selection is required for blood bank staff registration"
                );
            }

            pendingBloodBank = bloodBankRepository
                .findById(request.getBloodBankId())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Blood bank not found with id: " +
                        request.getBloodBankId()
                    )
                );

            if (!pendingBloodBank.getActive()) {
                throw new IllegalArgumentException(
                    "Selected blood bank is inactive"
                );
            }

            approvalStatus = User.ApprovalStatus.PENDING;
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .pendingHospital(pendingHospital)
            .assignedHospital(null)
            .pendingBloodBank(pendingBloodBank)
            .assignedBloodBank(null)
            .approvalStatus(approvalStatus)
            .build();

        userRepository.save(user);

        UserDetails userDetails =
            userDetailsService.loadUserByUsername(user.getUsername());

        String token =
            jwtTokenProvider.generateToken(userDetails);

        return buildJwtResponse(token, user);
    }

    /**
     * Build JWT response.
     */
    private JwtResponse buildJwtResponse(
        String token,
        User user
    ) {

        Long assignedHospitalId =
            user.getAssignedHospital() != null
                ? user.getAssignedHospital().getId()
                : null;

        String assignedHospitalName =
            user.getAssignedHospital() != null
                ? user.getAssignedHospital().getName()
                : null;

        Long pendingHospitalId =
            user.getPendingHospital() != null
                ? user.getPendingHospital().getId()
                : null;

        String pendingHospitalName =
            user.getPendingHospital() != null
                ? user.getPendingHospital().getName()
                : null;

        Long assignedBloodBankId =
            user.getAssignedBloodBank() != null
                ? user.getAssignedBloodBank().getId()
                : null;

        String assignedBloodBankName =
            user.getAssignedBloodBank() != null
                ? user.getAssignedBloodBank().getName()
                : null;

        Long pendingBloodBankId =
            user.getPendingBloodBank() != null
                ? user.getPendingBloodBank().getId()
                : null;

        String pendingBloodBankName =
            user.getPendingBloodBank() != null
                ? user.getPendingBloodBank().getName()
                : null;

        String approvalStatus =
            user.getApprovalStatus() != null
                ? user.getApprovalStatus().name()
                : ((user.getAssignedHospital() != null ||
                    user.getAssignedBloodBank() != null)
                    ? "APPROVED"
                    : null);

        return new JwtResponse(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().name(),
            approvalStatus,
            assignedHospitalId,
            assignedHospitalName,
            pendingHospitalId,
            pendingHospitalName,
            assignedBloodBankId,
            assignedBloodBankName,
            pendingBloodBankId,
            pendingBloodBankName
        );
    }
}
