package com.medion.hardwarestore.integration.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("emailNotificationService")
public class EmailService implements NotificationEventPublisher {

    @Override
    public void sendNotification(String recipient, String message) {
        log.info("Sending Email to {}: {}", recipient, message);
        // Stub: Connect to SMTP or SES here
    }
}
