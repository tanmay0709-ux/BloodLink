package com.bloodlink.config;

import com.bloodlink.model.User;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean hasAdmin = userRepository.findAll().stream()
            .anyMatch(u -> u.getRole() == User.Role.SYSTEM_ADMIN);

        if (!hasAdmin) {
            User admin = User.builder()
                .username("admin")
                .email("admin@bloodlink.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.SYSTEM_ADMIN)
                .build();
            userRepository.save(admin);
            logger.info("Created default SYSTEM_ADMIN user: 'admin' with password 'admin123'");

            
        }

        if (hasAdmin) {
                User admin = userRepository.findByUsername("admin").orElseThrow();

                logger.info("ADMIN FOUND: {}", admin.getUsername());
                logger.info("PASSWORD MATCHES admin123: {}",
                passwordEncoder.matches("admin123", admin.getPassword()));
        }
    }
}
