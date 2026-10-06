package com.school.counseling.module.auth.repository;

import com.school.counseling.module.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndPurposeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
            String email, String purpose, LocalDateTime now
    );
}
