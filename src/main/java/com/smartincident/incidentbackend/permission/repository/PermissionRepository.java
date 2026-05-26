package com.smartincident.incidentbackend.permission.repository;

import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.permission.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByName(Permission name);
}
