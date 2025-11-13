package com.smartincident.incidentbackend.notification.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.notification.entity.DeviceToken;
import com.smartincident.incidentbackend.notification.service.DeviceTokenService;
import com.smartincident.incidentbackend.utils.Response;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@GraphQLApi
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Authenticated
    @GraphQLMutation(name = "registerDeviceToken", description = "Register device token for push notifications")
    public Response<DeviceToken> registerToken(
            @GraphQLArgument(name = "userUid") String userUid,
            @GraphQLArgument(name = "token") String token,
            @GraphQLArgument(name = "deviceType", defaultValue = "FLUTTER") String deviceType,
            @GraphQLArgument(name = "appVersion", defaultValue = "1.0.0") String appVersion
    ) {
        return deviceTokenService.registerToken(userUid, token, deviceType, appVersion);
    }

    @Authenticated
    @GraphQLMutation(name = "removeDeviceToken", description = "Remove device token")
    public Response<Boolean> removeToken(@GraphQLArgument(name = "token") String token) {
        return deviceTokenService.removeToken(token);
    }
}