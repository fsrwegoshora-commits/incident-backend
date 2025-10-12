package com.smartincident.incidentbackend.authotp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // disable CSRF for POST
                .cors(cors -> cors.disable()) // disable CORS for local testing
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/graphql").permitAll() // ruhusu GraphQL bila token
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
