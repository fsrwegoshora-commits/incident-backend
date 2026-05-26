package com.smartincident.incidentbackend.authotp.controller;

import com.smartincident.incidentbackend.authotp.dto.AuthResponse;
import com.smartincident.incidentbackend.authotp.service.JwtService;
import com.smartincident.incidentbackend.authotp.service.OtpService;
import com.smartincident.incidentbackend.utils.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Citizen-specific authentication endpoints.
 *
 * POST /api/citizen/send-otp      — request OTP (phone number)
 * POST /api/citizen/verify-otp    — verify OTP → access + refresh tokens
 * POST /api/citizen/refresh-token — exchange refresh token for new access token
 */
@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
public class CitizenController {

    private final OtpService otpService;
    private final JwtService jwtService;

    @PostMapping("/send-otp")
    public Response<String> sendOtp(@RequestParam String phoneNumber) {
        return otpService.generateOtp(phoneNumber);
    }

    @PostMapping("/verify-otp")
    public Response<AuthResponse> verifyOtp(
            @RequestParam String phoneNumber,
            @RequestParam String code,
            @RequestHeader(value = "X-Device-Id",   required = false) String deviceId,
            @RequestHeader(value = "X-Device-Name", required = false) String deviceName,
            @RequestHeader(value = "X-Platform",    required = false) String platform,
            HttpServletRequest request) {
        try {
            String ip = resolveIp(request);
            AuthResponse auth = otpService.loginWithOtp(phoneNumber, code, deviceId, deviceName, platform, ip);
            return Response.success(auth);
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public Response<String> refreshToken(@RequestParam String refreshToken) {
        try {
            return Response.success(jwtService.refreshAccessToken(refreshToken));
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    private static String resolveIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
