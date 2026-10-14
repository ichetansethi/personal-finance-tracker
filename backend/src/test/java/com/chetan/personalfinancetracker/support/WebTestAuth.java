package com.chetan.personalfinancetracker.support;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.chetan.personalfinancetracker.security.JwtUtil;

import static org.mockito.Mockito.when;

public final class WebTestAuth {

    private WebTestAuth() {
    }

    public static void stubUser(UserDetailsService userDetailsService, String username) {
        UserDetails details = User.withUsername(username).password("ignored").roles("USER").build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(details);
    }

    public static String bearer(JwtUtil jwtUtil, String username) {
        return "Bearer " + jwtUtil.generateToken(username);
    }
}
