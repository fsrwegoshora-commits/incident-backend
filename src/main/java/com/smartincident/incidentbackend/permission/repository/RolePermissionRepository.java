package com.smartincident.incidentbackend.permission.repository;

import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.permission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(String role);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :role AND rp.permission.name = :permission")
    Optional<RolePermission> findByRoleAndPermission(@Param("role") String role,
                                                     @Param("permission") Permission permission);

    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role AND rp.permission.name = :permission")
    void deleteByRoleAndPermissionName(@Param("role") String role,
                                      @Param("permission") Permission permission);

    @Modifying
    @Transactional
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :role")
    void deleteByRole(@Param("role") String role);
}
