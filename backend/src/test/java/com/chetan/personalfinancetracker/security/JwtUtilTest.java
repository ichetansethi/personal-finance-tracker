package com.chetan.personalfinancetracker.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET", SECRET);
        jwtUtil.init();
    }

    @Test
    void generateTokenCarriesTheUsernameIssuerAndOneDayExpiry() {
        String token = jwtUtil.generateToken("alice");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.getIssuer()).isEqualTo("personal-finance-app");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(86_400_000L);
    }

    @Test
    void validateTokenRejectsTamperedAndForeignTokens() {
        String token = jwtUtil.generateToken("alice");
        String tampered = token.substring(0, token.length() - 2) + "aa";

        SecretKey otherKey = Keys.hmacShaKeyFor("fedcba9876543210fedcba9876543210".getBytes());
        String foreign = Jwts.builder()
                .setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
        assertThat(jwtUtil.validateToken(foreign)).isFalse();
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateTokenRejectsExpiredTokensWhileExtractUsernameThrows() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expired = Jwts.builder()
                .setSubject("alice")
                .setIssuer("personal-finance-app")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        assertThat(jwtUtil.validateToken(expired)).isFalse();
        assertThatThrownBy(() -> jwtUtil.extractUsername(expired))
                .isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> jwtUtil.extractUsername("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
