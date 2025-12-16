package com.smartincident.incidentbackend.setting.repository;

import com.smartincident.incidentbackend.setting.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByUid(String departmentUid);

    boolean existsByName(String trim);

    @Query("select d from Department d where (:isActive is null or d.isActive = :isActive) and " +
            "(lower(d.name) like %:key% or lower(d.type) like %:key% or lower(d.agency.name) like %:key%)")
    Page<Department> getDepartments(Pageable pageable, Boolean isActive, String key);
}
