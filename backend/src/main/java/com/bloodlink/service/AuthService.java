package com.bloodlink.service;

import com.bloodlink.dto.JwtResponse;
import com.bloodlink.dto.LoginRequest;
import com.bloodlink.dto.RegisterRequest;
import com.bloodlink.model.User;
import com.bloodlink.repository.UserRepository;
import com.bloodlink.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticate a user and return a JWT response.
     */
    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        return new JwtResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            user.getRole().name()
        );
    }

    /**
     * Register a new user account.
     */
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already registered");
        }

        // Determine role (default to VIEWER)
        User.Role role = User.Role.VIEWER;
        if (request.getRole() != null) {
            try {
                role = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Unknown role — fall back to VIEWER
            }
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .build();

        userRepository.save(user);

        // Auto-login: generate token for the new user
        String token = jwtTokenProvider.generateToken(user);

        return new JwtResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            user.getRole().name()
        );
    }
}
