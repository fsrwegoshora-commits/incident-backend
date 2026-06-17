package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.authotp.service.JwtService;
import com.smartincident.incidentbackend.utils.LoggedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    if (jwtService.isTokenValid(token)) {
                        String phone = jwtService.extractPhone(token);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(phone, null, List.of());
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        User user = userRepository.findByPhoneNumberWithAssociations(phone).orElse(null);
                        if (user != null) {
                            LoggedUser.setCurrent(user);
                        }
                    }
                } catch (Exception e) {
                    log.debug("JWT validation failed for request {}: {}", request.getRequestURI(), e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            LoggedUser.clearCurrent();
        }
    }
}
