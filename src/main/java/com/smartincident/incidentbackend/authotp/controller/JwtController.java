package com.smartincident.incidentbackend.authotp.controller;

import com.smartincident.incidentbackend.authotp.service.JwtService;
import com.smartincident.incidentbackend.utils.Response;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@GraphQLApi
@Controller
@RequiredArgsConstructor
public class JwtController {
    private final JwtService jwtService;
    @GraphQLQuery(name = "validateToken")
    public Response<Boolean> validateToken(@GraphQLArgument(name = "token") String token) {
        try {
            boolean isValid = jwtService.isTokenValid(token);
            return Response.success(isValid);
        } catch (Exception e) {
            return Response.error("Token validation failed");
        }
    }
}
