package com.chetan.personalfinancetracker.controller;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chetan.personalfinancetracker.config.CorsConfig;
import com.chetan.personalfinancetracker.config.PasswordEncoderConfig;
import com.chetan.personalfinancetracker.config.SecurityConfig;
import com.chetan.personalfinancetracker.dto.AuthRequest;
import com.chetan.personalfinancetracker.exception.BadRequestException;
import com.chetan.personalfinancetracker.exception.GlobalExceptionHandler;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void registerReturnsSuccessWithoutRequiringAToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret1","email":"alice@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(authService).register(any(AuthRequest.class));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidRegistrations")
    void registerRejectsInvalidInput(String body, String message) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));

        verify(authService, never()).register(any());
    }

    @Test
    void registerReportsADuplicateUsername() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("Username already exists"))
                .when(authService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret1","email":"alice@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void loginReturnsATokenWhenOtpIsNotRequired() throws Exception {
        when(authService.login(any())).thenReturn(false);
        when(authService.generateTokenWithoutOtp("alice")).thenReturn("signed-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret1","email":"alice@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(false))
                .andExpect(jsonPath("$.token").value("signed-token"));
    }

    @Test
    void loginTellsTheClientToCheckEmailWhenOtpIsRequired() throws Exception {
        when(authService.login(any())).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpRequired").value(true))
                .andExpect(jsonPath("$.message").value("OTP has been sent to your email"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(authService, never()).generateTokenWithoutOtp(any());
    }

    @Test
    void loginRejectsBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("nope"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginRejectsABlankPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: Password is required"));

        verify(authService, never()).login(any());
    }

    @Test
    void verifyOtpReturnsTheIssuedToken() throws Exception {
        when(authService.verifyOtpAndGenerateToken("alice", "123456")).thenReturn("signed-token");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","otp":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-token"));
    }

    @Test
    void verifyOtpReportsAnInvalidCode() throws Exception {
        when(authService.verifyOtpAndGenerateToken("alice", "000000"))
                .thenThrow(new BadRequestException("Invalid or expired OTP"));

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","otp":"000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    private static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("{\"password\":\"secret1\",\"email\":\"alice@example.com\"}", "username: Username is required"),
                Arguments.of("{\"username\":\"alice\",\"email\":\"alice@example.com\"}", "password: Password is required"),
                Arguments.of("{\"username\":\"alice\",\"password\":\"secret1\",\"email\":\"not-an-email\"}", "email: Email must be valid"));
    }
}
