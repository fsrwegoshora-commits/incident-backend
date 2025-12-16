package com.smartincident.incidentbackend.setting.service;

import com.smartincident.incidentbackend.setting.dto.DepartmentDto;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.entity.Department;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.setting.repository.DepartmentRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final AgencyRepository agencyRepository;

    public Response<Department> saveDepartment(DepartmentDto dto) {

        if (dto == null)
            return Response.error("Department data is required");

        log.info("User {} attempting to save department data", LoggedUser.getName());

        // Validate required fields
        if (dto.getName() == null || dto.getName().trim().isEmpty())
            return Response.error("Department name is required");

        if (dto.getType() == null)
            return Response.error("Department type is required");

        if (dto.getAgencyUid() == null || dto.getAgencyUid().trim().isEmpty())
            return Response.error("Agency UID is required");

        // Lookup agency
        Optional<Agency> agencyOpt = agencyRepository.findByUid(dto.getAgencyUid());
        if (agencyOpt.isEmpty())
            return Response.error("Agency not found with UID: " + dto.getAgencyUid());

        Agency agency = agencyOpt.get();

        Department department;

        // Update mode
        if (dto.getUid() != null) {
            Optional<Department> existing = departmentRepository.findByUid(dto.getUid());
            if (existing.isEmpty())
                return Response.error("Department not found with UID: " + dto.getUid());

            department = existing.get();
        } else {
            // Create mode
            if (departmentRepository.existsByName(dto.getName().trim())) {
                return Response.error("Department name already exists");
            }
            department = new Department();
        }

        department.setName(dto.getName().trim());
        department.setType(dto.getType());
        department.setAgency(agency);

        department.update();

        try {
            Department saved = departmentRepository.save(department);
            log.info("Department {} saved successfully", saved.getName());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Error saving department", e);
            return Response.error("Failed to save department");
        }
    }

    public Response<Department> deleteDepartment(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");

        Optional<Department> deptOpt = departmentRepository.findByUid(uid);
        if (deptOpt.isEmpty())
            return new Response<>("Invalid department provided");

        Department department = deptOpt.get();

        if (!department.getIsActive())
            return new Response<>("Department already deleted");

        department.delete();

        try {
            departmentRepository.save(department);
            log.info("Department deleted successfully: {}", department.getName());
            return Response.success(department);
        } catch (Exception e) {
            log.error("Failed to delete department: {}", e.getMessage());
            return new Response<>(Utils.getExceptionMessage(e));
        }
    }

    public Response<Department> getDepartmentByUid(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");

        Optional<Department> deptOpt = departmentRepository.findByUid(uid);
        if (deptOpt.isEmpty())
            return new Response<>("Invalid department provided");

        return Response.success(deptOpt.get());
    }
    public ResponsePage<Department> getDepartments(PageableParam pageableParam) {
        return new ResponsePage<>(
                departmentRepository.getDepartments(
                        pageableParam.getPageable(true),
                        pageableParam.getIsActive(),
                        pageableParam.key()
                )
        );
    }

}
