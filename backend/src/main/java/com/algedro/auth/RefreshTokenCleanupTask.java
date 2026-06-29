package com.algedro.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Scheduled(cron = "0 0 2 * * ?") // Ejecutar a las 2 AM todos los días
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
}