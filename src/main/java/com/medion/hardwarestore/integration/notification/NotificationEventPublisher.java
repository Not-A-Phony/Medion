package com.medion.hardwarestore.integration.notification;

public interface NotificationEventPublisher {
    void sendNotification(String recipient, String message);
}
