package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final UserRepository userRepository;

    @Before("@annotation(com.smartincident.incidentbackend.authotp.security.Authenticated)")
    public void beforeAuthenticatedMethod() {
        jwtAuthInterceptor.checkAuth(); // basic token validation
    }

    @Before("@annotation(authorizedRole)")
    public void beforeAuthorizedMethod(AuthorizedRole authorizedRole) {
        String phone = jwtAuthInterceptor.extractPhoneFromRequest();
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role userRole = user.getRole();
        boolean allowed = Arrays.stream(authorizedRole.value())
                .anyMatch(r -> r == userRole);

        if (!allowed) {
            throw new RuntimeException("Access denied for role: " + userRole);
        }
    }
}
