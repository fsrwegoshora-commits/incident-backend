package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret}")
    private String secretKeyStr;

    @Value("${jwt.expiration.access:86400000}")
    private long accessTokenExpiry;

    @Value("${jwt.expiration.refresh:604800000}")
    private long refreshTokenExpiry;

    private Key secretKey;

    @PostConstruct
    public void init() {
        // Ensure the secret is at least 32 bytes for HMAC-SHA256
        byte[] keyBytes = secretKeyStr.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getPhoneNumber())
                .claim("role", user.getRole().name())
                .claim("station_id", user.getStation() != null ? user.getStation().getUid() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getPhoneNumber())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    public String refreshAccessToken(String refreshToken) {
        Claims claims = extractAllClaims(refreshToken);

        // Ensure it's actually a refresh token
        if (!"refresh".equals(claims.get("type"))) {
            throw new RuntimeException("Invalid refresh token");
        }

        if (isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token has expired, please log in again");
        }

        String phone = claims.getSubject();
        User user = userRepository.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseThrow(() -> new RuntimeException("User not found or inactive"));

        return generateToken(user);
    }

    public String extractPhone(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public String extractStationId(String token) {
        return (String) extractAllClaims(token).get("station_id");
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token) {
        try {
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.info("Token is blacklisted");
                return false;
            }

            String phoneNumber = extractPhone(token);

            if (tokenBlacklistService.isUserBlacklisted(phoneNumber)) {
                log.info("User is blacklisted: {}", phoneNumber);
                return false;
            }

            Optional<User> oUser = userRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber);
            if (oUser.isEmpty()) {
                log.info("User account not found or inactive: {}", phoneNumber);
                return false;
            }

            // Validates signature and expiry in one call
            Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token);

            return !isTokenExpired(token);

        } catch (JwtException e) {
            return false;
        }
    }

    public void invalidateToken(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                tokenBlacklistService.blacklistToken(token, expiration.getTime());
                log.info("Token invalidated");
            }
        } catch (JwtException e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
        }
    }

    public void invalidateAllUserTokens(String phoneNumber) {
        tokenBlacklistService.blacklistUser(phoneNumber);
        log.info("All tokens invalidated for user: {}", phoneNumber);
    }

    public void removeUserFromBlacklist(String phoneNumber) {
        tokenBlacklistService.removeUserFromBlacklist(phoneNumber);
    }
}
