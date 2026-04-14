package com.smartincident.incidentbackend.authotp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/debug/**").permitAll()
                        .requestMatchers("/ws-notifications/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Public auth endpoints
                        .requestMatchers("/api/auth/otp/request").permitAll()
                        .requestMatchers("/api/auth/otp/verify").permitAll()
                        .requestMatchers("/api/auth/token/validate").permitAll()
                        .requestMatchers("/api/auth/token/refresh").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        // Public user registration
                        .requestMatchers("/api/users/register").permitAll()
                        // Public reference data
                        .requestMatchers("/api/areas/**").permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
