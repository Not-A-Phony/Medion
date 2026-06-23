package com.medion.hardwarestore.integration.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("whatsappNotificationService")
public class WhatsAppService implements NotificationEventPublisher {

    @Override
    public void sendNotification(String recipient, String message) {
        log.info("Sending WhatsApp to {}: {}", recipient, message);
        // Stub: Call Green API or Twilio API here
    }
}
