package com.chetan.personalfinancetracker.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundUses404AndTheRequestPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/transactions");

        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("User not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/transactions");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void badRequestUses400() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");

        ResponseEntity<ApiError> response = handler.handleBadRequest(
                new BadRequestException("Username already exists"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Username already exists");
    }

    @Test
    void authenticationFailuresUseAGeneric401Message() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        ResponseEntity<ApiError> credentials = handler.handleAuthenticationError(
                new BadCredentialsException("password mismatch"), request);
        ResponseEntity<ApiError> missing = handler.handleAuthenticationError(
                new UsernameNotFoundException("User not found: alice"), request);

        assertThat(credentials.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(credentials.getBody().getMessage()).isEqualTo("Invalid username or password");
        assertThat(missing.getBody().getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    void unexpectedExceptionsAreHiddenBehindAGeneric500() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/summary/monthly");

        ResponseEntity<ApiError> response = handler.handleGeneric(new IllegalStateException("db down"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("db down");
    }
}
