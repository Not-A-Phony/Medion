package com.medion.hardwarestore.service;

import com.medion.hardwarestore.controller.auth.AuthRequest;
import com.medion.hardwarestore.controller.auth.AuthResponse;
import com.medion.hardwarestore.domain.user.Role;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import com.medion.hardwarestore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void testLogin() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin@medion.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setEmail("admin@medion.com");
        user.setRole(Role.ADMIN);

        when(userRepository.findByUsernameOrEmail("admin@medion.com", "admin@medion.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any())).thenReturn("mockToken");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
    }
}
