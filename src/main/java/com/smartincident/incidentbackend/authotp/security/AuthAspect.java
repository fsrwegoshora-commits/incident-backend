package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.permission.service.PermissionService;
import com.smartincident.incidentbackend.utils.LoggedUser;
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

    private final PermissionService permissionService;

    /**
     * Reads the User cached by JwtAuthFilter for this request (LoggedUser) instead of
     * re-querying the DB on every @Authenticated/@RequiresPermission check.
     * LoggedUser.get() itself falls back to a DB lookup if the cache is empty (e.g. non-HTTP
     * contexts), so behavior is unchanged when the cache wasn't populated.
     */
    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User user = LoggedUser.get();
        if (user == null) {
            throw new AccessDeniedException("Authenticated user not found");
        }
        return user;
    }

    @Before("@annotation(com.smartincident.incidentbackend.authotp.security.Authenticated)")
    public void beforeAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
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
