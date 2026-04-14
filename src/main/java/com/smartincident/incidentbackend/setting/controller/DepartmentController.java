package com.smartincident.incidentbackend.setting.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.setting.dto.DepartmentDto;
import com.smartincident.incidentbackend.setting.entity.Department;
import com.smartincident.incidentbackend.setting.service.DepartmentService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @PostMapping
    public Response<Department> saveDepartment(@RequestBody DepartmentDto departmentDto) {
        return departmentService.saveDepartment(departmentDto);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<Department> deleteDepartment(@PathVariable String uid) {
        return departmentService.deleteDepartment(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.STATION_ADMIN})
    @GetMapping("/{uid}")
    public Response<Department> getDepartment(@PathVariable String uid) {
        return departmentService.getDepartmentByUid(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.STATION_ADMIN})
    @GetMapping
    public ResponsePage<Department> getDepartments(@ModelAttribute PageableParam pageableParam) {
        return departmentService.getDepartments(pageableParam != null ? pageableParam : new PageableParam());
    }
}
