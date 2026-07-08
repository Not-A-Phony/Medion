package com.medion.hardwarestore.controller.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

    private String email;
    
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
    
    public String getIdentifier() {
        return (username != null && !username.trim().isEmpty()) ? username : email;
    }
}
