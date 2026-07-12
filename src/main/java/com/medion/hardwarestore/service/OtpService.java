package com.medion.hardwarestore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    // Ideally, JavaMailSender would be injected if spring-boot-starter-mail is present
    // private final JavaMailSender mailSender;
    
    // In-memory OTP store for demonstration (in production, use Redis)
    private final Map<String, String> otpStore = new HashMap<>();

    public void generateAndSendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(email, otp);
        
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(email);
        // message.setSubject("Your Verification OTP");
        // message.setText("Your OTP is: " + otp);
        // mailSender.send(message);
        
        System.out.println("OTP for " + email + " is " + otp); // Mock sending
    }

    public boolean verifyOtp(String email, String otp) {
        String storedOtp = otpStore.get(email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(email);
            return true;
        }
        return false;
    }
}
