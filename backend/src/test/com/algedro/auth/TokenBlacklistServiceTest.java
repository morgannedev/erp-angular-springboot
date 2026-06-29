package com.algedro.auth;

import com.algedro.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    void testBlacklistToken() {
        String token = "test-token";
        long expirationTime = System.currentTimeMillis() + 3600000; // 1 hora
        
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }

    @Test
    void testTokenNoBlacklisted() {
        assertThat(tokenBlacklistService.isBlacklisted("unknown-token")).isFalse();
    }

    @Test
    void testTokenExpiradoSeLimpia() throws InterruptedException {
        String token = "expired-token";
        long expirationTime = System.currentTimeMillis() + 100; // 100ms
        
        tokenBlacklistService.blacklistToken(token, expirationTime);
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
        
        Thread.sleep(150);
        
        assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }

    @Test
    void testCleanExpiredTokens() {
        String validToken = "valid-token";
        String expiredToken = "expired-token";
        
        tokenBlacklistService.blacklistToken(validToken, System.currentTimeMillis() + 3600000);
        tokenBlacklistService.blacklistToken(expiredToken, System.currentTimeMillis() - 1000);
        
        tokenBlacklistService.cleanExpiredTokens();
        
        assertThat(tokenBlacklistService.isBlacklisted(validToken)).isTrue();
        assertThat(tokenBlacklistService.isBlacklisted(expiredToken)).isFalse();
    }
}