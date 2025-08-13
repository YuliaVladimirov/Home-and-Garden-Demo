package org.example.homeandgarden.email.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String subject, String body);
}
