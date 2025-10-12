package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.service.OfficerShiftService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@GraphQLApi
@RequiredArgsConstructor
public class OfficerShiftController {
    private final OfficerShiftService officerShiftService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "saveShift", description = "New police officer shift")
    public Response<OfficerShift> saveShift(@GraphQLArgument(name = "policeOfficerDto") OfficerShiftDto officerShiftDto) {
        return officerShiftService.saveShift(officerShiftDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "excuseShift", description = "save shift excuse")
    public Response<OfficerShift> excuseShift(@GraphQLArgument(name = "policeOfficerDto") String shitUid,@GraphQLArgument(name="reason") String reason) {
        return officerShiftService.excuseShift(shitUid,reason);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "deleteOfficerShift", description = "Deletes a police officer shift using it's uid")
    public Response<OfficerShift> deleteOfficerShift(@GraphQLArgument(name = "uid") String uid) {
        return officerShiftService.deleteOfficerShift(uid);
    }
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "reassignShift", description = "save shift reassign")
    public Response<OfficerShift> reassignShift(@GraphQLArgument(name = "policeOfficerDto") String shitUid,@GraphQLArgument(name="newOfficerUid") String newOfficerUid) {
        return officerShiftService.reassignShift(shitUid,newOfficerUid);
    }
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getPoliceOfficerShifts", description = "Gets a page of police officer shift")
    public ResponsePage<OfficerShift> getPoliceOfficerShifts(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam) {
        return officerShiftService.getPoliceOfficerShifts(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT,Role.POLICE_OFFICER})
    @GraphQLQuery(name = "getPoliceOfficerShift", description = "Gets a police officer shift using it's uid")
    public Response<OfficerShift> getPoliceOfficerShift(@GraphQLArgument(name = "uid") String uid) {
        return officerShiftService.getPoliceOfficerShift(uid);
    }
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getShiftsByStation", description = "Gets a page of Officer Shift")
    public ResponsePage<OfficerShift> getShiftsByStation(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam, @GraphQLArgument(name = "policeStationUid") String policeStationUid) {
        return officerShiftService.getShiftsByStation(pageableParam != null ? pageableParam : new PageableParam(), policeStationUid);
    }
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT,Role.POLICE_OFFICER})
    @GraphQLQuery(name = "getShiftsByPoliceOfficer", description = "Gets a page of Officer Shift")
    public ResponsePage<OfficerShift> getShiftsByPoliceOfficer(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam, @GraphQLArgument(name = "policeOfficerUid") String policeOfficerUid) {
        return officerShiftService.getShiftsByPoliceOfficer(pageableParam != null ? pageableParam : new PageableParam(), policeOfficerUid);
    }
}
