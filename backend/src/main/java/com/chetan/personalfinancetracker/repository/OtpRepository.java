package com.chetan.personalfinancetracker.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.chetan.personalfinancetracker.model.Otp;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByUsernameAndOtpAndUsedFalse(String username, String otp);

    Optional<Otp> findFirstByUsernameAndUsedFalseOrderByExpiryTimeDesc(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM Otp o WHERE o.expiryTime < ?1")
    void deleteExpiredOtps(LocalDateTime now);

    @Modifying
    @Transactional
    void deleteByUsername(String username);
}
