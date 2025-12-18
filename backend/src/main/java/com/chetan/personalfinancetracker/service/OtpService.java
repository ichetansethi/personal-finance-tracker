package com.chetan.personalfinancetracker.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chetan.personalfinancetracker.exception.BadRequestException;
import com.chetan.personalfinancetracker.model.Otp;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.OtpRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${otp.enabled}")
    private boolean otpEnabled;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    public boolean isOtpEnabled() {
        return otpEnabled;
    }

    @Transactional
    public void generateAndSendOtp(String username) {
        if (!otpEnabled) {
            logger.info("OTP is disabled. Skipping OTP generation.");
            return;
        }

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Delete any existing OTPs for this user
        otpRepository.deleteByUsername(username);

        // Generate OTP
        String otpCode = generateOtpCode();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        // Save OTP
        Otp otp = new Otp(username, otpCode, expiryTime);
        otpRepository.save(otp);

        // Send email
        emailService.sendOtpEmail(user.getEmail(), username, otpCode);

        logger.info("OTP generated and sent for user: {}", username);
    }

    @Transactional
    public boolean verifyOtp(String username, String otpCode) {
        if (!otpEnabled) {
            logger.info("OTP is disabled. Skipping OTP verification.");
            return true; // If OTP is disabled, always return true
        }

        // Find the OTP
        Optional<Otp> otpOpt = otpRepository.findByUsernameAndOtpAndUsedFalse(username, otpCode);

        if (otpOpt.isEmpty()) {
            logger.warn("Invalid OTP attempt for user: {}", username);
            return false;
        }

        Otp otp = otpOpt.get();

        // Check if expired
        if (otp.isExpired()) {
            logger.warn("Expired OTP attempt for user: {}", username);
            return false;
        }

        // Mark as used
        otp.setUsed(true);
        otpRepository.save(otp);

        logger.info("OTP verified successfully for user: {}", username);
        return true;
    }

    private String generateOtpCode() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    // Cleanup expired OTPs every hour
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredOtps() {
        logger.info("Cleaning up expired OTPs...");
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }
}
