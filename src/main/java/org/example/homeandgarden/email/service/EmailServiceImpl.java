package org.example.homeandgarden.email.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String userName;

    @Override
    public void sendPasswordResetEmail(String toEmail, String subject, String body) {


        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(userName);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText("To reset your password, please click the link below:\n" + body);

        mailSender.send(message);
    }
}
