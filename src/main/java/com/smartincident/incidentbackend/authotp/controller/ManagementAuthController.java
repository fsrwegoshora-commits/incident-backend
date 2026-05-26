package com.smartincident.incidentbackend.authotp.controller;

import com.smartincident.incidentbackend.authotp.dto.AuthResponse;
import com.smartincident.incidentbackend.authotp.service.JwtService;
import com.smartincident.incidentbackend.authotp.service.ManagementAuthService;
import com.smartincident.incidentbackend.utils.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Management user authentication — Username + Password.
 *
 * POST /api/auth/login          — accepts { username, password }
 * POST /api/auth/refresh-token  — exchange refresh token for new access token
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ManagementAuthController {

    private final ManagementAuthService managementAuthService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public Response<AuthResponse> login(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Device-Id",   required = false) String deviceId,
            @RequestHeader(value = "X-Device-Name", required = false) String deviceName,
            @RequestHeader(value = "X-Platform",    required = false) String platform,
            HttpServletRequest request) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String ip = resolveIp(request);
            AuthResponse auth = managementAuthService.login(
                    username, password, deviceId, deviceName, platform, ip);
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
