package com.chetan.personalfinancetracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chetan.personalfinancetracker.mapper.UserMapper;
import com.chetan.personalfinancetracker.model.User;
import com.chetan.personalfinancetracker.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return "Hello, " + userDetails.getUsername() + "! This is your profile.";
    }

    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

}