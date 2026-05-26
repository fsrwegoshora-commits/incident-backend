package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.dto.AuthResponse;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.permission.service.PermissionService;
import com.smartincident.incidentbackend.session.service.DeviceSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagementAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PermissionService permissionService;
    private final DeviceSessionService deviceSessionService;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Authenticates a management user with username + password.
     * Throws RuntimeException with a descriptive message on failure.
     */
    public AuthResponse login(String username, String password,
                              String deviceId, String deviceName,
                              String platform, String ipAddress) {

        if (username == null || username.isBlank())
            throw new RuntimeException("Username is required");
        if (password == null || password.isBlank())
            throw new RuntimeException("Password is required");

        User user = userRepository.findByUsernameAndIsActiveTrue(username.trim())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (user.getPasswordHash() == null)
            throw new RuntimeException("This account uses OTP authentication");

        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            throw new RuntimeException("Invalid username or password");

        // Citizens must use OTP — block management-login for citizens
        if (user.getRole() == Role.CITIZEN)
            throw new RuntimeException("Citizen accounts must use the citizen login");

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Set<Permission> permissions = permissionService.getPermissionsForRole(user.getRole());

        deviceSessionService.recordLogin(
                user.getUid(), user.getName(),
                user.getRole() != null ? user.getRole().name() : null,
                deviceId, deviceName, platform, ipAddress);

        log.info("Management login: {} ({})", username, user.getRole());

        return new AuthResponse(
                accessToken, refreshToken,
                user.getPhoneNumber(), user.getUsername(),
                user.getRole(),
                user.getEmergencyUnit() != null ? user.getEmergencyUnit().getUid() : null,
                permissions);
    }
}
