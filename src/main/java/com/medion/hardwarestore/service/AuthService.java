package com.medion.hardwarestore.service;

import com.medion.hardwarestore.controller.auth.AuthRequest;
import com.medion.hardwarestore.controller.auth.AuthResponse;
import com.medion.hardwarestore.controller.auth.RegisterRequest;
import com.medion.hardwarestore.domain.user.Role;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import com.medion.hardwarestore.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already taken");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(assignedRole)
                .build();
        
        userRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("userId", user.getId());

        var jwtToken = jwtService.generateToken(extraClaims, user);
        
        return AuthResponse.builder()
                .accessToken(jwtToken)
                .profile(AuthResponse.UserProfile.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("userId", user.getId());

        var jwtToken = jwtService.generateToken(extraClaims, user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .profile(AuthResponse.UserProfile.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
