package com.medion.hardwarestore.controller.auth;

import com.medion.hardwarestore.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private UserProfile profile;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserProfile {
        private UUID id;
        private String username;
        private String email;
        private Role role;
    }
}
