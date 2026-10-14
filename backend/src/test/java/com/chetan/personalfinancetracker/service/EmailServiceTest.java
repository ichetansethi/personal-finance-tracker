package com.chetan.personalfinancetracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void configureSender() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "otpEnabled", true);
    }

    @Test
    void sendOtpEmailBuildsAMessageContainingTheCode() {
        emailService.sendOtpEmail("alice@example.com", "alice", "123456");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getValue().getTo()).containsExactly("alice@example.com");
        assertThat(message.getValue().getSubject()).isEqualTo("Finance Tracker - Login OTP");
        assertThat(message.getValue().getText()).contains("alice").contains("123456");
    }

    @Test
    void sendOtpEmailWrapsMailFailures() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendOtpEmail("alice@example.com", "alice", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send OTP email. Please try again.");
    }

    @Test
    void sendOtpEmailIsSkippedWhenOtpIsDisabled() {
        ReflectionTestUtils.setField(emailService, "otpEnabled", false);

        emailService.sendOtpEmail("alice@example.com", "alice", "123456");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
