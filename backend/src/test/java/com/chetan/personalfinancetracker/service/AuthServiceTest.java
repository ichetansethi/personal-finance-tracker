package com.chetan.personalfinancetracker.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chetan.personalfinancetracker.dto.AuthRequest;
import com.chetan.personalfinancetracker.exception.BadRequestException;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.UserRepository;
import com.chetan.personalfinancetracker.security.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerEncodesPasswordAndPersistsNewUser() {
        AuthRequest request = request("alice", "plain-secret", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-secret")).thenReturn("$2a$hashed");

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("$2a$hashed");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        AuthRequest request = request("alice", "plain-secret", "alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginReturnsFalseWhenCredentialsAreValidAndOtpIsDisabled() {
        AuthRequest request = request("alice", "plain-secret", null);
        when(otpService.isOtpEnabled()).thenReturn(false);

        boolean otpRequired = authService.login(request);

        assertThat(otpRequired).isFalse();
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(otpService, never()).generateAndSendOtp(any());
    }

    @Test
    void loginSendsOtpAndReturnsTrueWhenOtpIsEnabled() {
        AuthRequest request = request("alice", "plain-secret", null);
        when(otpService.isOtpEnabled()).thenReturn(true);

        boolean otpRequired = authService.login(request);

        assertThat(otpRequired).isTrue();
        verify(otpService).generateAndSendOtp("alice");
    }

    @Test
    void loginDoesNotSendOtpWhenCredentialsAreRejected() {
        AuthRequest request = request("alice", "wrong", null);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(otpService, never()).isOtpEnabled();
        verify(otpService, never()).generateAndSendOtp(any());
    }

    @Test
    void verifyOtpIssuesTokenForTheLoadedAccount() {
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("alice").password("hash").roles("USER").build();
        when(otpService.verifyOtp("alice", "123456")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
        when(jwtUtil.generateToken("alice")).thenReturn("signed-token");

        assertThat(authService.verifyOtpAndGenerateToken("alice", "123456")).isEqualTo("signed-token");
    }

    @Test
    void verifyOtpRejectsInvalidOrExpiredCode() {
        when(otpService.verifyOtp("alice", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyOtpAndGenerateToken("alice", "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired OTP");

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void generateTokenWithoutOtpUsesTheLoadedUsername() {
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("alice").password("hash").roles("USER").build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);
        when(jwtUtil.generateToken("alice")).thenReturn("direct-token");

        assertThat(authService.generateTokenWithoutOtp("alice")).isEqualTo("direct-token");
    }

    @Test
    void generateTokenWithoutOtpPropagatesUnknownUsers() {
        when(userDetailsService.loadUserByUsername("missing"))
                .thenThrow(new UsernameNotFoundException("User not found: missing"));

        assertThatThrownBy(() -> authService.generateTokenWithoutOtp("missing"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(jwtUtil, never()).generateToken(any());
    }

    private static AuthRequest request(String username, String password, String email) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        return request;
    }
}
