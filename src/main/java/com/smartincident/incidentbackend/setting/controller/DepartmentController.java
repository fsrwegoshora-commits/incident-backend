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
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@GraphQLApi
@Slf4j
@Controller
@RequiredArgsConstructor
public class DepartmentController {
    private  final DepartmentService departmentService;

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @GraphQLMutation(name = "saveDepartment", description = "Saving department information")
    public Response<Department>saveDepartment(@GraphQLArgument(name = "departmentDto") DepartmentDto departmentDto){
        return departmentService.saveDepartment(departmentDto);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @GraphQLMutation(name = "deleteDepartment", description = "Deleting department information")
    public Response<Department>deleteDepartment(@GraphQLArgument(name = "uid") String uid){
        return departmentService.deleteDepartment(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT,Role.STATION_ADMIN})
    @GraphQLQuery(name = "getDepartment", description = "Getting department information")
    public Response<Department> getDepartment(@GraphQLArgument(name = "uid") String uid){
        return departmentService.getDepartmentByUid(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT,Role.STATION_ADMIN})
    @GraphQLQuery(name = "getDepartments", description = "Getting departments information")
    public ResponsePage<Department>getDepartments(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam){
        return departmentService.getDepartments(pageableParam!=null?pageableParam:new PageableParam());
    }
}
