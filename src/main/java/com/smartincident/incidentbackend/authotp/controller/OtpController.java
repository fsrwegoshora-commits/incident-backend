package com.smartincident.incidentbackend.authotp.controller;

import com.smartincident.incidentbackend.authotp.dto.AuthResponse;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.authotp.service.JwtService;
import com.smartincident.incidentbackend.authotp.service.OtpService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;
    private final JwtService jwtService;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * Step 1 — Request an OTP for the given phone number.
     * Returns the OTP in the response only when app.otp.expose-in-response=true (dev mode).
     */
    @PostMapping("/otp/request")
    public Response<String> requestOtp(@RequestParam String phoneNumber) {
        return otpService.generateOtp(phoneNumber);
    }

    /**
     * Step 2 — Verify OTP and receive access + refresh tokens.
     */
    @PostMapping("/otp/verify")
    public Response<AuthResponse> verifyOtp(@RequestParam String phoneNumber, @RequestParam String code) {
        try {
            AuthResponse auth = otpService.loginWithOtp(phoneNumber, code);
            return Response.success(auth);
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    /**
     * Refresh an expired access token using a valid refresh token.
     */
    @PostMapping("/token/refresh")
    public Response<String> refreshToken(@RequestParam String refreshToken) {
        try {
            String newAccessToken = jwtService.refreshAccessToken(refreshToken);
            return Response.success(newAccessToken);
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    /**
     * Validate an access token.
     */
    @GetMapping("/token/validate")
    public Response<Boolean> validateToken(@RequestParam String token) {
        try {
            return Response.success(jwtService.isTokenValid(token));
        } catch (Exception e) {
            return Response.error("Token validation failed");
        }
    }

    /**
     * Logout — invalidates the current access token.
     * Requires Authorization: Bearer <token> header.
     */
    @PostMapping("/logout")
    public Response<String> logout() {
        try {
            String token = jwtAuthInterceptor.extractTokenFromRequest();
            jwtService.invalidateToken(token);
            return Response.success("Logged out successfully");
        } catch (Exception e) {
            return Response.error("Logout failed: " + e.getMessage());
        }
    }
}
