package com.medion.hardwarestore.integration.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("whatsappNotificationService")
public class WhatsAppService implements NotificationEventPublisher {

    @Override
    public void sendNotification(String recipient, String message) {
        log.info("Preparing WhatsApp payload for Green API...");
        String payload = String.format("{\n  \"chatId\": \"%s@c.us\",\n  \"message\": \"%s\"\n}", recipient, message);
        log.info("Sending HTTP POST to Green API /waInstance<id>/sendMessage endpoint with payload: \n{}", payload);
        
        // Simulating network delay for mock
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("WhatsApp message successfully delivered via Green API mock to {}", recipient);
    }
}
