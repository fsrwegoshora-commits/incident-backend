package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final UserRepository userRepository;
    private final PermissionService permissionService;

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        String phone = (String) auth.getPrincipal();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    @Before("@annotation(com.smartincident.incidentbackend.authotp.security.Authenticated)")
    public void beforeAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    @Before("@annotation(authorizedRole)")
    public void beforeAuthorizedRole(AuthorizedRole authorizedRole) {
        User user = resolveCurrentUser();
        Role userRole = user.getRole();
        boolean allowed = Arrays.stream(authorizedRole.value()).anyMatch(r -> r == userRole);
        if (!allowed) {
            throw new AccessDeniedException("Access denied: role " + userRole + " is not permitted");
        }
    }

    @Before("@annotation(requiresPermission)")
    public void beforeRequiresPermission(RequiresPermission requiresPermission) {
        User user = resolveCurrentUser();
        Set<Permission> userPermissions = permissionService.getPermissionsForRole(user.getRole());
        Set<Permission> required = Set.of(requiresPermission.value());

        boolean allowed = requiresPermission.requireAll()
                ? userPermissions.containsAll(required)
                : required.stream().anyMatch(userPermissions::contains);

        if (!allowed) {
            throw new AccessDeniedException(
                    "Missing required permission(s): " + Arrays.toString(requiresPermission.value()));
        }
    }
}
