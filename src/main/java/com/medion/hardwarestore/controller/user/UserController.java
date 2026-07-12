package com.medion.hardwarestore.controller.user;

import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record UserProfileUpdateRequest(String firstName, String lastName, String email) {}
    public record UserProfileResponse(UUID id, String firstName, String lastName, String email, String phoneNumber, String role) {}

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody UserProfileUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        User updatedUser = userService.updateProfile(user, request.firstName(), request.lastName(), request.email());
        UserProfileResponse response = new UserProfileResponse(
                updatedUser.getId(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                updatedUser.getEmail(),
                updatedUser.getPhoneNumber(),
                updatedUser.getRole().name()
        );
        return ResponseEntity.ok(response);
    }
}
