package com.analyzerservice.notification;

public interface NotificationService {
    void send(String serviceName, String severity, String analysis);
}
