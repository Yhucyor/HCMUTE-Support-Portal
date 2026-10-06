package com.school.counseling.module.auth.service;

import com.school.counseling.module.auth.entity.OtpToken;
import com.school.counseling.module.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRATION_MINUTES = 10;

    @Transactional
    public String generateOtp(String email, String purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);

        OtpToken token = OtpToken.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expiredAt(expiresAt)
                .isUsed(false)
                .build();

        otpTokenRepository.save(token);
        log.info("Generated OTP [{}] for email [{}] with purpose [{}]", code, email, purpose);
        return code;
    }

    @Transactional
    public boolean verifyOtp(String email, String code, String purpose) {
        return otpTokenRepository.findTopByEmailAndPurposeAndIsUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
                email, purpose, LocalDateTime.now()
        ).filter(token -> token.getOtpCode().equals(code))
         .map(token -> {
             token.setIsUsed(true);
             otpTokenRepository.save(token);
             return true;
         }).orElse(false);
    }
}
