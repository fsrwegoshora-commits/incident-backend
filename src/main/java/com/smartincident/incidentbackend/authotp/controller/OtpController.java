package com.smartincident.incidentbackend.authotp.controller;

import com.smartincident.incidentbackend.authotp.service.JwtService;
import com.smartincident.incidentbackend.authotp.service.OtpService;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.utils.Response;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@GraphQLApi
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GraphQLMutation(name = "requestOtp")
    public Response<String> requestOtp(@GraphQLArgument(name = "phoneNumber") String phoneNumber) {
        return otpService.generateOtp(phoneNumber);
    }

    @GraphQLMutation(name = "verifyOtp")
    public Response<String> verifyOtp(@GraphQLArgument(name = "phoneNumber") String phoneNumber, @GraphQLArgument(name = "code") String code) {
        try {
            String token = String.valueOf(otpService.loginWithOtp(phoneNumber, code));
            return Response.success(token);
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }
}