package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    // secret key ya JWT
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getPhoneNumber())
                .claim("role", user.getRole().name())
                .claim("station_id", user.getStation() != null ? user.getStation().getUid() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SECRET_KEY)
                .compact();
    }

    public String extractPhone(String token) {
        return extractAllClaims(token).getSubject(); // subject = phoneNumber
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public String extractStationId(String token) {
        return (String) extractAllClaims(token).get("station_id");
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getPhoneNumber())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 604800000)) // 7 days
                .signWith(SECRET_KEY)
                .compact();
    }

    public String refreshToken(String refreshToken) {
        String phone = extractPhone(refreshToken);
        return generateToken(userRepository.findByPhoneNumber(phone).orElseThrow(() -> new RuntimeException("User not found")));
    }

    public boolean isTokenValid(String token) {
        try {
            // 1. Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.info("Token is blacklisted");
                return false;
            }

            // 2. Check if user is blacklisted
            String phoneNumber = extractPhone(token);
            if (tokenBlacklistService.isUserBlacklisted(phoneNumber)) {
                log.info("User is blacklisted: {}", phoneNumber);
                return false;
            }

            Optional<User> oUser = userRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber);
            if (oUser.isEmpty() || !oUser.get().getIsActive()) {
                log.info("User account deleted or inactive: {}", phoneNumber);
                return false;
            }

            // 3. Check standard JWT validation
            Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);

            // 4. Check if token expired
            return !isTokenExpired(token);

        } catch (JwtException e) {
            return false;
        }
    }

    // Invalidate specific token (for logout)
    public void invalidateToken(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                tokenBlacklistService.blacklistToken(token, expiration.getTime());
                log.info("Token invalidated: {}", token.substring(0, 10) + "...");
            }
        } catch (JwtException e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
        }
    }

    // Invalidate all tokens for user (for account deletion)
    public void invalidateAllUserTokens(String phoneNumber) {
        tokenBlacklistService.blacklistUser(phoneNumber);
        log.info("All tokens invalidated for user: {}", phoneNumber);
    }

    public void removeUserFromBlacklist(String phoneNumber) {
        tokenBlacklistService.removeUserFromBlacklist(phoneNumber);
        log.info("User removed from JWT blacklist: {}", phoneNumber);
    }
}
