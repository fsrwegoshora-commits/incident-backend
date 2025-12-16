package com.smartincident.incidentbackend.setting.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.setting.dto.AgencyDto;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.service.AgencyService;
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

@Controller
@Slf4j
@GraphQLApi
@RequiredArgsConstructor
public class AgencyController {
    private final AgencyService agencyService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN})
    @GraphQLMutation(name = "saveAgency", description = "Saving agency information")
    public Response<Agency>saveAgency(@GraphQLArgument(name = "agencyDto") AgencyDto agencyDto){
        return agencyService.saveAgency(agencyDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN})
    @GraphQLQuery(name = "getAgency", description = "Getting agency information")
    public Response<Agency> getAgency(@GraphQLArgument(name = "uid") String uid){
        return agencyService.getAgencyByUid(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN})
    @GraphQLMutation(name = "deleteAgency", description = "Deleting agency information")
    public Response<Agency>deleteAgency(@GraphQLArgument(name = "uid") String uid){
        return agencyService.deleteAgency(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN})
    @GraphQLQuery(name = "getAgencies", description = "Getting agencies information")
    public ResponsePage<Agency>getAgencies(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam){
        return agencyService.getAgencies(pageableParam!=null?pageableParam:new PageableParam());
    }
}

