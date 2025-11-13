package com.smartincident.incidentbackend.notification.service;

import com.smartincident.incidentbackend.notification.entity.DeviceToken;
import com.smartincident.incidentbackend.notification.repository.DeviceTokenRepository;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public Response<DeviceToken> registerToken(String userUid, String token, String deviceType, String appVersion) {
        try {
            Optional<User> userOpt = userRepository.findByUid(userUid);
            if (userOpt.isEmpty()) {
                return Response.error("User not found");
            }

            User user = userOpt.get();

            // Check if token already exists
            Optional<DeviceToken> existingTokenOpt = deviceTokenRepository.findByToken(token);

            DeviceToken deviceToken;
            if (existingTokenOpt.isPresent()) {
                // Update existing token
                deviceToken = existingTokenOpt.get();
                deviceToken.setUser(user);
                deviceToken.setDeviceType(deviceType);
                deviceToken.setAppVersion(appVersion);
                deviceToken.setLastUsedAt(LocalDateTime.now());
                deviceToken.setIsActive(true);
            } else {
                // Create new token
                deviceToken = new DeviceToken();
                deviceToken.setUser(user);
                deviceToken.setToken(token);
                deviceToken.setDeviceType(deviceType != null ? deviceType : "FLUTTER");
                deviceToken.setAppVersion(appVersion);
                deviceToken.setLastUsedAt(LocalDateTime.now());
            }

            DeviceToken saved = deviceTokenRepository.save(deviceToken);
            log.info(" Device token registered for user: {}", userUid);
            return Response.success(saved);

        } catch (Exception e) {
            log.error(" Failed to register device token: {}", e.getMessage());
            return Response.error("Failed to register device token");
        }
    }

    public List<String> getActiveTokensByUserUid(String userUid) {
        return deviceTokenRepository.findActiveTokensByUserUid(userUid);
    }

    @Transactional
    public Response<Boolean> removeToken(String token) {
        try {
            deviceTokenRepository.deactivateByToken(token);
            log.info("Device token deactivated: {}", token);
            return Response.success(true);
        } catch (Exception e) {
            log.error("Failed to remove device token: {}", e.getMessage());
            return Response.error("Failed to remove device token");
        }
    }
}