package com.medion.hardwarestore.service;

import com.medion.hardwarestore.controller.auth.AuthRequest;
import com.medion.hardwarestore.controller.auth.AuthResponse;
import com.medion.hardwarestore.controller.auth.UserProfile;
import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import com.medion.hardwarestore.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.medion.hardwarestore.security.RefreshTokenService;
import com.medion.hardwarestore.domain.user.RefreshToken;
import com.medion.hardwarestore.controller.auth.RegisterRequest;
import com.medion.hardwarestore.controller.auth.TokenRefreshRequest;
import com.medion.hardwarestore.controller.auth.TokenRefreshResponse;
import com.medion.hardwarestore.domain.user.Role;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String jwtToken = jwtService.generateToken(user);
        
        // Delete old refresh tokens and create a new one
        refreshTokenService.deleteByUserId(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserProfile profile = UserProfile.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .profile(profile)
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getIdentifier()).isPresent()) {
            throw new IllegalArgumentException("Username/Email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getIdentifier())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserProfile profile = UserProfile.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .profile(profile)
                .build();
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
