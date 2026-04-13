package com.analyzerservice.notification;

import org.springframework.stereotype.Component;

@Component
public class EmailNotificationService implements NotificationService {

    private final EmailSender emailSender;

    public EmailNotificationService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void send(String message) {
        emailSender.send(message);
    }
}
