package com.chetan.personalfinancetracker.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.chetan.personalfinancetracker.dto.AuthRequest;
import com.chetan.personalfinancetracker.dto.OtpVerificationRequest;
import com.chetan.personalfinancetracker.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthRequest request) {
        boolean otpRequired = authService.login(request);

        Map<String, Object> body = new HashMap<>();

        if (otpRequired) {
            // OTP is enabled, OTP has been sent to email
            body.put("otpRequired", true);
            body.put("message", "OTP has been sent to your email");
        } else {
            // OTP is disabled, return token directly
            String token = authService.generateTokenWithoutOtp(request.getUsername());
            body.put("token", token);
            body.put("otpRequired", false);
        }

        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody OtpVerificationRequest request) {
        String token = authService.verifyOtpAndGenerateToken(request.getUsername(), request.getOtp());

        Map<String, String> body = new HashMap<>();
        body.put("token", token);

        return ResponseEntity.ok(body);
    }
}