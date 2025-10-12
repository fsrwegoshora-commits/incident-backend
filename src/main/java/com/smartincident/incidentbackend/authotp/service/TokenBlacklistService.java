package com.smartincident.incidentbackend.authotp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> blacklistedUsers = new ConcurrentHashMap<>();

    // Blacklist specific token
    public void blacklistToken(String token, long expiresAt) {
        blacklistedTokens.put(token, expiresAt);
        log.info("Token blacklisted: {} (expires: {})",
                token.substring(0, 10) + "...", new Date(expiresAt));
    }

    // Blacklist all tokens for a user
    public void blacklistUser(String phoneNumber) {
        long expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        blacklistedUsers.put(phoneNumber, expiresAt);
        log.info("User blacklisted: {}", phoneNumber);
    }

    // Check if token is blacklisted
    public boolean isTokenBlacklisted(String token) {
        Long expiresAt = blacklistedTokens.get(token);
        if (expiresAt == null) return false;

        // Remove if expired
        if (System.currentTimeMillis() > expiresAt) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    // Check if user is blacklisted
    public boolean isUserBlacklisted(String phoneNumber) {
        Long expiresAt = blacklistedUsers.get(phoneNumber);
        if (expiresAt == null) return false;

        // Remove if expired
        if (System.currentTimeMillis() > expiresAt) {
            blacklistedUsers.remove(phoneNumber);
            return false;
        }
        return true;
    }

    // Cleanup expired entries every 5 minutes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        int tokensCleaned = 0;
        int usersCleaned = 0;

        // Clean expired tokens
        for (Map.Entry<String, Long> entry : blacklistedTokens.entrySet()) {
            if (now > entry.getValue()) {
                blacklistedTokens.remove(entry.getKey());
                tokensCleaned++;
            }
        }

        // Clean expired users
        for (Map.Entry<String, Long> entry : blacklistedUsers.entrySet()) {
            if (now > entry.getValue()) {
                blacklistedUsers.remove(entry.getKey());
                usersCleaned++;
            }
        }

        if (tokensCleaned > 0 || usersCleaned > 0) {
            log.debug("Cleaned up {} expired tokens and {} expired users",
                    tokensCleaned, usersCleaned);
        }
    }

    public void removeUserFromBlacklist(String phoneNumber) {
        blacklistedUsers.remove(phoneNumber);
        log.info("User removed from blacklist for re-registration: {}", phoneNumber);
    }

    // OPTIONAL: Remove specific token from blacklist
    public void removeTokenFromBlacklist(String token) {
        blacklistedTokens.remove(token);
        log.info("Token removed from blacklist: {}", token.substring(0, 10) + "...");
    }
}