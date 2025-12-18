package com.chetan.personalfinancetracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.chetan.personalfinancetracker.dto.AuthRequest;
import com.chetan.personalfinancetracker.exception.BadRequestException;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtUtil;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    public void register(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        userRepository.save(user);
    }

    public boolean login(AuthRequest request) {
        // Step 1: Authenticate credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()
            )
        );

        // Step 2: Check if OTP is enabled
        if (otpService.isOtpEnabled()) {
            // Generate and send OTP
            otpService.generateAndSendOtp(request.getUsername());
            return false; // OTP required
        } else {
            // OTP disabled, return token directly
            return true; // No OTP required
        }
    }

    public String verifyOtpAndGenerateToken(String username, String otp) {
        // Verify OTP
        boolean isValid = otpService.verifyOtp(username, otp);

        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Generate and return JWT
        return jwtUtil.generateToken(userDetails.getUsername());
    }

    public String generateTokenWithoutOtp(String username) {
        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Generate and return JWT
        return jwtUtil.generateToken(userDetails.getUsername());
    }
}