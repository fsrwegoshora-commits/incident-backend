package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.authotp.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor {

    private final JwtService jwtService;


    public void checkAuth() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            throw new RuntimeException("Token is invalid or expired");
        }
    }

    public String extractPhoneFromRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        return jwtService.extractPhone(token);
    }

    public String extractTokenFromRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        return authHeader.substring(7);
    }


    public String getValidatedPhoneNumber() {
        String token = extractTokenFromRequest();

        if (!jwtService.isTokenValid(token)) {
            throw new RuntimeException("Token is invalid or expired");
        }

        return jwtService.extractPhone(token);
    }
}
