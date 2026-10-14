package com.chetan.personalfinancetracker.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.chetan.personalfinancetracker.exception.BadRequestException;
import com.chetan.personalfinancetracker.model.Otp;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.repository.OtpRepository;
import com.chetan.personalfinancetracker.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void configureOtp() {
        ReflectionTestUtils.setField(otpService, "otpEnabled", true);
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
    }

    @Test
    void reportsWhetherOtpIsEnabled() {
        assertThat(otpService.isOtpEnabled()).isTrue();

        ReflectionTestUtils.setField(otpService, "otpEnabled", false);
        assertThat(otpService.isOtpEnabled()).isFalse();
    }

    @Test
    void generateDoesNothingWhenOtpIsDisabled() {
        ReflectionTestUtils.setField(otpService, "otpEnabled", false);

        otpService.generateAndSendOtp("alice");

        verifyNoInteractions(userRepository, otpRepository, emailService);
    }

    @Test
    void generateRejectsUnknownUsers() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.generateAndSendOtp("missing"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User not found");

        verify(otpRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void generateReplacesPreviousCodeAndEmailsANumericOtp() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        otpService.generateAndSendOtp("alice");

        ArgumentCaptor<Otp> saved = ArgumentCaptor.forClass(Otp.class);
        InOrder order = inOrder(otpRepository, emailService);
        order.verify(otpRepository).deleteByUsername("alice");
        order.verify(otpRepository).save(saved.capture());
        order.verify(emailService).sendOtpEmail("alice@example.com", "alice", saved.getValue().getOtp());

        Otp otp = saved.getValue();
        assertThat(otp.getUsername()).isEqualTo("alice");
        assertThat(otp.getOtp()).matches("\\d{6}");
        assertThat(otp.isUsed()).isFalse();
        assertThat(otp.getExpiryTime()).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(otp.getExpiryTime()).isBefore(LocalDateTime.now().plusMinutes(6));
    }

    @Test
    void verifyAcceptsAnyCodeWhenOtpIsDisabled() {
        // Current behaviour: a disabled OTP check returns true without looking at the code.
        // AuthService.verifyOtpAndGenerateToken will then issue a JWT. See the PR notes.
        ReflectionTestUtils.setField(otpService, "otpEnabled", false);

        assertThat(otpService.verifyOtp("alice", "not-a-real-code")).isTrue();

        verifyNoInteractions(otpRepository);
    }

    @Test
    void verifyRejectsUnknownCodes() {
        when(otpRepository.findByUsernameAndOtpAndUsedFalse("alice", "000000")).thenReturn(Optional.empty());

        assertThat(otpService.verifyOtp("alice", "000000")).isFalse();

        verify(otpRepository, never()).save(any());
    }

    @Test
    void verifyRejectsExpiredCodesWithoutConsumingThem() {
        Otp expired = new Otp("alice", "123456", LocalDateTime.now().minusMinutes(1));
        when(otpRepository.findByUsernameAndOtpAndUsedFalse("alice", "123456")).thenReturn(Optional.of(expired));

        assertThat(otpService.verifyOtp("alice", "123456")).isFalse();
        assertThat(expired.isUsed()).isFalse();
        verify(otpRepository, never()).save(any());
    }

    @Test
    void verifyConsumesAMatchingUnexpiredCode() {
        Otp otp = new Otp("alice", "123456", LocalDateTime.now().plusMinutes(5));
        when(otpRepository.findByUsernameAndOtpAndUsedFalse("alice", "123456")).thenReturn(Optional.of(otp));

        assertThat(otpService.verifyOtp("alice", "123456")).isTrue();
        assertThat(otp.isUsed()).isTrue();
        verify(otpRepository).save(otp);
    }

    @Test
    void cleanupDeletesExpiredRows() {
        otpService.cleanupExpiredOtps();

        verify(otpRepository).deleteExpiredOtps(any(LocalDateTime.class));
        verify(otpRepository, never()).deleteByUsername(anyString());
    }
}
