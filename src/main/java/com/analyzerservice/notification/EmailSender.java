package com.analyzerservice.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String to;

    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String serviceName, String severity, String analysis) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
    
        String subject = String.format(
                "[ALERT][%s][%s] %s",
                serviceName,
                severity
        );
    
        mail.setSubject(subject);

    
        String body = String.format(
            "[요약]\n%s\n\n%s",
            analysis
        );
    
        mail.setText(body);
    
        mailSender.send(mail);
    }
    
    
}
