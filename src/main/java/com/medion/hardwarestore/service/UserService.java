package com.medion.hardwarestore.service;

import com.medion.hardwarestore.domain.user.User;
import com.medion.hardwarestore.domain.user.UserRepository;
import com.medion.hardwarestore.exception.BusinessException;
import com.medion.hardwarestore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User updateProfile(User user, String firstName, String lastName, String email) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!existingUser.getEmail().equals(email)) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new BusinessException("Email is already in use by another account");
            }
        }

        existingUser.setFirstName(firstName);
        existingUser.setLastName(lastName);
        existingUser.setEmail(email);

        return userRepository.save(existingUser);
    }
}
