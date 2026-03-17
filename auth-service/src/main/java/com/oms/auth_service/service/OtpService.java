package com.oms.auth_service.service;

import com.oms.auth_service.entity.Otp;
import com.oms.auth_service.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    @Value("${app.otp.validity-minutes:5}")
    private Integer otpValidityMinutes;

    @Value("${app.otp.max-attempts:5}")
    private Integer maxAttempts;

    /**
     * Generate and send OTP to the provided email
     */
    public void generateAndSendOtp(String email) {
        // Delete any existing unverified OTPs for this email
        otpRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // Create OTP entity
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setCode(otpCode);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(otpValidityMinutes));
        otp.setIsVerified(false);
        otp.setAttemptCount(0);
        otp.setCreatedAt(LocalDateTime.now());

        otpRepository.save(otp);

        // Send email
        emailService.sendOtpEmail(email, otpCode);
        log.info("OTP generated and sent to email: {}", email);
    }

    /**
     * Verify the OTP code for the given email
     */
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        Otp otp = otpRepository.findByEmailAndCode(email, otpCode)
                .orElseThrow(() -> new RuntimeException("Invalid OTP code"));

        if (otp.isExpired()) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (otp.getAttemptCount() >= maxAttempts) {
            otpRepository.delete(otp);
            throw new RuntimeException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        otp.setIsVerified(true);
        otpRepository.save(otp);
        log.info("OTP verified successfully for email: {}", email);
    }

    /**
     * Validate if OTP exists and is not expired
     */
    public boolean isOtpValid(String email, String otpCode) {
        return otpRepository.findByEmailAndCode(email, otpCode)
                .map(otp -> !otp.isExpired() && !otp.getIsVerified())
                .orElse(false);
    }

    /**
     * Check if OTP is already verified
     */
    public boolean isOtpVerified(String email) {
        return otpRepository.findLatestUnverifiedByEmail(email)
                .map(Otp::getIsVerified)
                .orElse(false);
    }

    /**
     * Increment attempt count for wrong OTP
     */
    @Transactional
    public void incrementAttempt(String email, String otpCode) {
        otpRepository.findByEmailAndCode(email, otpCode).ifPresent(otp -> {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
        });
    }

    /**
     * Delete OTP after successful registration
     */
    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email);
        log.info("OTP deleted for email: {}", email);
    }

    /**
     * Scheduled task to clean up expired OTPs (runs every hour)
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredOtps() {
        try {
            otpRepository.deleteExpiredOtps(LocalDateTime.now());
            log.info("Expired OTPs cleaned up successfully");
        } catch (Exception e) {
            log.error("Error while cleaning up expired OTPs", e);
        }
    }
}

