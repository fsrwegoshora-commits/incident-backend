package com.smartincident.incidentbackend.session.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.session.entity.DeviceSession;
import com.smartincident.incidentbackend.session.service.DeviceSessionService;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class DeviceSessionController {

    private final DeviceSessionService service;

    /** Current user's active sessions. */
    @Authenticated
    @GetMapping("/my")
    public Response<List<DeviceSession>> mySessions() {
        return service.getActiveSessions(LoggedUser.getUid());
    }

    /** Admin: paginated session history for any user. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_AUDIT_LOGS)
    @GetMapping
    public ResponsePage<DeviceSession> getAllSessions(
            @RequestParam(required = false) String userUid,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.getSessions(userUid, active, page, size);
    }

    /** Logout a specific device session (self or admin). */
    @Authenticated
    @DeleteMapping("/{deviceId}")
    public Response<String> logoutDevice(@PathVariable String deviceId) {
        service.logoutDevice(LoggedUser.getUid(), deviceId);
        return Response.success("Session terminated");
    }

    /** Logout all devices (self). */
    @Authenticated
    @DeleteMapping("/all")
    public Response<String> logoutAll() {
        service.logoutAll(LoggedUser.getUid());
        return Response.success("All sessions terminated");
    }
}
