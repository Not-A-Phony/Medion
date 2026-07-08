package com.medion.hardwarestore.config;

import com.medion.hardwarestore.domain.user.Role;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String superAdminUsername = "mathu";
        Optional<User> existingAdmin = userRepository.findByEmail(superAdminUsername);

        if (existingAdmin.isEmpty()) {
            log.info("Super Admin 'mathu' not found. Creating...");
            User superAdmin = User.builder()
                    .email(superAdminUsername)
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Role.ADMIN)
                    .phoneNumber("0000000000") // Required but can be mock data
                    .build();
            userRepository.save(superAdmin);
            log.info("Super Admin 'mathu' created successfully.");
        } else {
            log.info("Super Admin 'mathu' already exists. Skipping creation.");
        }
    }
}
