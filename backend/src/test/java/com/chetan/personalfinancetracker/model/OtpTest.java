package com.chetan.personalfinancetracker.model;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpTest {

    @Test
    void newCodesAreUnusedAndExpireOnlyAfterTheDeadline() {
        Otp fresh = new Otp("alice", "123456", LocalDateTime.now().plusMinutes(5));
        Otp expired = new Otp("alice", "654321", LocalDateTime.now().minusSeconds(1));

        assertThat(fresh.isUsed()).isFalse();
        assertThat(fresh.isExpired()).isFalse();
        assertThat(expired.isExpired()).isTrue();
    }
}
