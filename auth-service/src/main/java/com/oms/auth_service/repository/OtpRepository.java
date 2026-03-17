package com.oms.auth_service.repository;

import com.oms.auth_service.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    @Query("SELECT o FROM Otp o WHERE o.email = ?1 AND o.isVerified = false ORDER BY o.createdAt DESC LIMIT 1")
    Optional<Otp> findLatestUnverifiedByEmail(String email);

    @Query("SELECT o FROM Otp o WHERE o.email = ?1 AND o.code = ?2 AND o.isVerified = false")
    Optional<Otp> findByEmailAndCode(String email, String code);

    @Query("SELECT o FROM Otp o WHERE o.expiryTime < ?1")
    List<Otp> findExpiredOtps(LocalDateTime now);

    @Query("DELETE FROM Otp o WHERE o.expiryTime < ?1")
    void deleteExpiredOtps(LocalDateTime now);

    void deleteByEmail(String email);
}

