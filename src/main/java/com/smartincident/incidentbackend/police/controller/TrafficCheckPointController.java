//////////////////////////////////////////////////////////////////////////////////////////controller
package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.TrafficCheckPointDto;
import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import com.smartincident.incidentbackend.police.service.TrafficCheckPointService;
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
public class TrafficCheckPointController {
    private  final TrafficCheckPointService trafficCheckPointService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "saveTrafficCheckpoint", description = "Save traffic checkpoint")
    public Response<TrafficCheckpoint>saveTrafficCheckpoint(@GraphQLArgument(name = "trafficCheckPointDto") TrafficCheckPointDto trafficCheckPointDto){
        return trafficCheckPointService.saveTrafficCheckpoint(trafficCheckPointDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "deleteTrafficCheckpoint", description = "Delete traffic checkpoint by uid")
    public Response<TrafficCheckpoint>deleteTrafficCheckpoint(@GraphQLArgument(name = "uid") String uid){
        return trafficCheckPointService.deleteTrafficCheckpoint(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getTrafficCheckpoint", description = "Get traffic checkpoint by")
    public Response<TrafficCheckpoint>getTrafficCheckpoint(@GraphQLArgument(name = "uid") String uid){
        return trafficCheckPointService.getTrafficCheckpointByUid(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getTrafficCheckpoints", description = "Get traffic checkpoints")
    public ResponsePage<TrafficCheckpoint>getTrafficCheckpoints(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam){
        return trafficCheckPointService.getCheckpoints(pageableParam!=null?pageableParam:new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getTrafficCheckpointsByPoliceStation", description = "Get traffic checkpoints")
    public ResponsePage<TrafficCheckpoint>getTrafficCheckpointsByPoliceStation(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam,@GraphQLArgument(name = "stationUid") String stationUid){
        return trafficCheckPointService.getTrafficCheckpointsByPoliceStation(pageableParam!=null?pageableParam:new PageableParam(),stationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "assignSupervisor", description = "Assign supervisor to traffic checkpoint")
    public Response<TrafficCheckpoint>assignSupervisor(@GraphQLArgument(name = "checkpointUid") String checkpointUid,
                                                       @GraphQLArgument(name = "officerUid") String officerUid){
        return trafficCheckPointService.assignSupervisor(checkpointUid,officerUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "changeSupervisor", description = "Change supervisor of traffic checkpoint")
    public Response<TrafficCheckpoint>changeSupervisor(@GraphQLArgument(name = "checkpointUid") String checkpointUid,
                                                       @GraphQLArgument(name = "officerUid") String newOfficerUid){
        return trafficCheckPointService.changeSupervisor(checkpointUid,newOfficerUid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT,Role.STATION_ADMIN})
    @GraphQLMutation(name = "activateOrDeactivateCheckpoint", description = "activate or deactivate  traffic checkpoint")
    public Response<TrafficCheckpoint>activateOrDeactivateCheckpoint(@GraphQLArgument(name = "checkpointUid") String checkpointUid){
        return trafficCheckPointService.activateOrDeactivateCheckpoint(checkpointUid);
    }
}