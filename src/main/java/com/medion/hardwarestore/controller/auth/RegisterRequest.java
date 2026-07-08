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
public class RegisterRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String email;
    
    private String username;
    
    public String getIdentifier() {
        return (username != null && !username.trim().isEmpty()) ? username : email;
    }

    @NotBlank(message = "Password is required")
    private String password;

    private String phoneNumber;
    
    private boolean isStoreOwner;
}
