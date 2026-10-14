package com.chetan.personalfinancetracker.controller;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chetan.personalfinancetracker.config.CorsConfig;
import com.chetan.personalfinancetracker.config.PasswordEncoderConfig;
import com.chetan.personalfinancetracker.config.SecurityConfig;
import com.chetan.personalfinancetracker.exception.GlobalExceptionHandler;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.security.JwtFilter;
import com.chetan.personalfinancetracker.security.JwtUtil;
import com.chetan.personalfinancetracker.service.UserService;
import com.chetan.personalfinancetracker.support.WebTestAuth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtFilter.class, JwtUtil.class, CorsConfig.class,
        GlobalExceptionHandler.class, PasswordEncoderConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.secret=0123456789abcdef0123456789abcdef")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubCaller() {
        WebTestAuth.stubUser(userDetailsService, "alice");
    }

    @Test
    void profileRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void profileGreetsTheTokenSubject() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, alice! This is your profile."));
    }

    @Test
    void lowercaseBearerSchemeIsNotAccepted() throws Exception {
        String token = jwtUtil.generateToken("alice");

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void malformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/users/profile"));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        String expired = Jwts.builder()
                .setSubject("alice")
                .setIssuer("personal-finance-app")
                .setIssuedAt(new Date(System.currentTimeMillis() - 172_800_000L))
                .setExpiration(new Date(System.currentTimeMillis() - 86_400_000L))
                .signWith(Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void currentUserHidesThePasswordHash() throws Exception {
        User alice = new User();
        alice.setId(4L);
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setPassword("$2a$hash");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(alice));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void currentUserReportsAnUnknownAccountAsUnauthorized() throws Exception {
        when(userService.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", WebTestAuth.bearer(jwtUtil, "alice")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
