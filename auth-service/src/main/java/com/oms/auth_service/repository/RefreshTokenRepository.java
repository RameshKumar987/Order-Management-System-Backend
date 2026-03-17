package com.oms.auth_service.repository;

import com.oms.auth_service.entity.RefreshToken;
import com.oms.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser(User user);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
    long deleteByExpiryDateBefore(Instant instant);
}

