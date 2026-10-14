package com.chetan.personalfinancetracker.repository;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.chetan.personalfinancetracker.model.Otp;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OtpRepositoryTest {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsOnlyAnUnusedMatchingCode() {
        otpRepository.save(new Otp("alice", "111111", LocalDateTime.now().plusMinutes(5)));
        Otp used = new Otp("alice", "222222", LocalDateTime.now().plusMinutes(5));
        used.setUsed(true);
        otpRepository.save(used);

        assertThat(otpRepository.findByUsernameAndOtpAndUsedFalse("alice", "111111")).isPresent();
        assertThat(otpRepository.findByUsernameAndOtpAndUsedFalse("alice", "222222")).isEmpty();
        assertThat(otpRepository.findByUsernameAndOtpAndUsedFalse("bob", "111111")).isEmpty();
    }

    @Test
    void latestUnusedCodeIsReturnedFirst() {
        otpRepository.save(new Otp("alice", "111111", LocalDateTime.now().plusMinutes(1)));
        otpRepository.save(new Otp("alice", "999999", LocalDateTime.now().plusMinutes(10)));

        assertThat(otpRepository.findFirstByUsernameAndUsedFalseOrderByExpiryTimeDesc("alice"))
                .get()
                .extracting(Otp::getOtp)
                .isEqualTo("999999");
    }

    @Test
    void deleteByUsernameLeavesOtherAccountsAlone() {
        otpRepository.save(new Otp("alice", "111111", LocalDateTime.now().plusMinutes(5)));
        otpRepository.save(new Otp("bob", "333333", LocalDateTime.now().plusMinutes(5)));
        entityManager.flush();
        entityManager.clear();

        otpRepository.deleteByUsername("alice");
        entityManager.flush();
        entityManager.clear();

        assertThat(otpRepository.findAll()).extracting(Otp::getUsername).containsExactly("bob");
    }

    @Test
    void deleteExpiredOtpsKeepsFreshCodes() {
        otpRepository.save(new Otp("alice", "111111", LocalDateTime.now().minusMinutes(5)));
        otpRepository.save(new Otp("alice", "222222", LocalDateTime.now().plusMinutes(5)));
        entityManager.flush();
        entityManager.clear();

        otpRepository.deleteExpiredOtps(LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(otpRepository.findAll()).extracting(Otp::getOtp).containsExactly("222222");
    }
}
