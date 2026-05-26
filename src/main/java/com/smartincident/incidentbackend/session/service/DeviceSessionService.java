package com.smartincident.incidentbackend.session.service;

import com.smartincident.incidentbackend.session.entity.DeviceSession;
import com.smartincident.incidentbackend.session.repository.DeviceSessionRepository;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSessionService {

    private final DeviceSessionRepository repository;

    /** Record a new login event (async — never blocks the login response). */
    @Async
    public void recordLogin(String userUid, String userName, String userRole,
                            String deviceId, String deviceName, String platform, String ipAddress) {
        try {
            // Deactivate any previous session for this device
            if (deviceId != null && !deviceId.isBlank()) {
                repository.logoutDevice(deviceId, userUid, LocalDateTime.now());
            }

            DeviceSession session = DeviceSession.builder()
                    .userUid(userUid)
                    .userName(userName)
                    .userRole(userRole)
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .platform(platform != null ? platform.toUpperCase() : "UNKNOWN")
                    .ipAddress(ipAddress)
                    .loginAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .build();
            repository.save(session);
        } catch (Exception e) {
            log.warn("Failed to record device session: {}", e.getMessage());
        }
    }

    /** Mark all active sessions for a user as logged out. */
    public void logoutAll(String userUid) {
        repository.logoutAllForUser(userUid, LocalDateTime.now());
    }

    /** Mark a specific device session as logged out. */
    public void logoutDevice(String userUid, String deviceId) {
        repository.logoutDevice(deviceId, userUid, LocalDateTime.now());
    }

    /** Get all active sessions for a user. */
    public Response<List<DeviceSession>> getActiveSessions(String userUid) {
        return Response.success(repository.findByUserUidAndActiveTrue(userUid));
    }

    /** Paginated session history (admin view). */
    public ResponsePage<DeviceSession> getSessions(String userUid, Boolean active, int page, int size) {
        return new ResponsePage<>(repository.findFiltered(userUid, active, PageRequest.of(page, size)));
    }
}
