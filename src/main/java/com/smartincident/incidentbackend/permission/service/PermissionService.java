package com.smartincident.incidentbackend.permission.service;

import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.permission.entity.PermissionEntity;
import com.smartincident.incidentbackend.permission.entity.RolePermission;
import com.smartincident.incidentbackend.permission.repository.PermissionRepository;
import com.smartincident.incidentbackend.permission.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Cacheable(value = "rolePermissions", key = "#role.name()")
    public Set<Permission> getPermissionsForRole(Role role) {
        return rolePermissionRepository.findByRole(role.name())
                .stream()
                .map(rp -> rp.getPermission().getName())
                .collect(Collectors.toSet());
    }

    public Map<String, List<String>> getAllRolePermissions() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            List<String> perms = rolePermissionRepository.findByRole(role.name())
                    .stream()
                    .map(rp -> rp.getPermission().getName().name())
                    .sorted()
                    .collect(Collectors.toList());
            result.put(role.name(), perms);
        }
        return result;
    }

    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#role.name()")
    public void addPermissionToRole(Role role, Permission permission) {
        if (rolePermissionRepository.findByRoleAndPermission(role.name(), permission).isPresent()) {
            return;
        }
        PermissionEntity pe = permissionRepository.findByName(permission)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permission));
        rolePermissionRepository.save(RolePermission.builder()
                .role(role.name())
                .permission(pe)
                .build());
    }

    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#role.name()")
    public void removePermissionFromRole(Role role, Permission permission) {
        rolePermissionRepository.deleteByRoleAndPermissionName(role.name(), permission);
    }

    public List<PermissionEntity> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public boolean hasPermission(Role role, Permission permission) {
        return getPermissionsForRole(role).contains(permission);
    }

    /** Replace all permissions for a role atomically. Evicts cache. */
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#role.name()")
    public void setPermissionsForRole(Role role, Set<Permission> permissions) {
        rolePermissionRepository.deleteByRole(role.name());
        for (Permission p : permissions) {
            PermissionEntity pe = permissionRepository.findByName(p)
                    .orElseThrow(() -> new RuntimeException("Permission not found: " + p));
            rolePermissionRepository.save(RolePermission.builder()
                    .role(role.name())
                    .permission(pe)
                    .build());
        }
    }

    /** Return permissions for a single role as a name-sorted list of strings. */
    public List<String> getPermissionNamesForRole(Role role) {
        return rolePermissionRepository.findByRole(role.name())
                .stream()
                .map(rp -> rp.getPermission().getName().name())
                .sorted()
                .collect(Collectors.toList());
    }
}
