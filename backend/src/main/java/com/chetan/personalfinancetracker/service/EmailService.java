package com.chetan.personalfinancetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${otp.enabled}")
    private boolean otpEnabled;

    public void sendOtpEmail(String toEmail, String username, String otp) {
        if (!otpEnabled) {
            logger.info("OTP is disabled. Skipping email send.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Finance Tracker - Login OTP");
            message.setText(buildOtpEmailBody(username, otp));

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to: {}. Error: {}", toEmail, e.getMessage());
            // For development, we'll just log the OTP
            logger.info("OTP for user {}: {}", username, otp);
        }
    }

    private String buildOtpEmailBody(String username, String otp) {
        return String.format(
            "Hello %s,\n\n" +
            "Your OTP for login is: %s\n\n" +
            "This OTP is valid for 5 minutes.\n\n" +
            "If you did not request this OTP, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Finance Tracker Team",
            username, otp
        );
    }
}
