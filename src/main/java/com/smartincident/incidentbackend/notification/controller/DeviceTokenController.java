package com.smartincident.incidentbackend.notification.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.notification.entity.DeviceToken;
import com.smartincident.incidentbackend.notification.service.DeviceTokenService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications/tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Authenticated
    @PostMapping
    public Response<DeviceToken> registerToken(
            @RequestParam String userUid,
            @RequestParam String token,
            @RequestParam(defaultValue = "FLUTTER") String deviceType,
            @RequestParam(defaultValue = "1.0.0") String appVersion) {
        return deviceTokenService.registerToken(userUid, token, deviceType, appVersion);
    }

    @Authenticated
    @DeleteMapping
    public Response<Boolean> removeToken(@RequestParam String token) {
        return deviceTokenService.removeToken(token);
    }
}
