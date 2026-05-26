package com.smartincident.incidentbackend.permission.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.permission.entity.PermissionEntity;
import com.smartincident.incidentbackend.permission.service.PermissionService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @RequiresPermission(Permission.VIEW_AUDIT_LOGS)
    public ResponseList<PermissionEntity> getAllPermissions() {
        return new ResponseList<>(permissionService.getAllPermissions());
    }

    @GetMapping("/roles")
    @RequiresPermission(Permission.VIEW_AUDIT_LOGS)
    public Response<Map<String, List<String>>> getRolePermissions() {
        return new Response<>(permissionService.getAllRolePermissions());
    }

    @PostMapping("/roles/{role}/permissions/{permission}")
    @RequiresPermission(Permission.ASSIGN_ROLE)
    public Response<String> addPermission(@PathVariable Role role, @PathVariable Permission permission) {
        permissionService.addPermissionToRole(role, permission);
        return Response.success("Permission " + permission + " added to role " + role);
    }

    @DeleteMapping("/roles/{role}/permissions/{permission}")
    @RequiresPermission(Permission.ASSIGN_ROLE)
    public Response<String> removePermission(@PathVariable Role role, @PathVariable Permission permission) {
        permissionService.removePermissionFromRole(role, permission);
        return Response.success("Permission " + permission + " removed from role " + role);
    }

    /** Get permissions currently assigned to a specific role. */
    @GetMapping("/roles/{role}")
    @Authenticated
    @RequiresPermission(Permission.VIEW_AUDIT_LOGS)
    public Response<List<String>> getRolePermissions(@PathVariable Role role) {
        return new Response<>(permissionService.getPermissionNamesForRole(role));
    }

    /**
     * Bulk-replace all permissions for a role in one call.
     * Sends the complete desired set; existing assignments are replaced atomically.
     */
    @PutMapping("/roles/{role}/permissions")
    @Authenticated
    @RequiresPermission(Permission.ASSIGN_ROLE)
    public Response<List<String>> setRolePermissions(@PathVariable Role role,
                                                     @RequestBody Set<Permission> permissions) {
        permissionService.setPermissionsForRole(role, permissions);
        return new Response<>(permissionService.getPermissionNamesForRole(role));
    }
}
