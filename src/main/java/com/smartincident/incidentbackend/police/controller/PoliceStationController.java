package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.authotp.service.UserService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.LocationDto;
import com.smartincident.incidentbackend.police.dto.PoliceStationDto;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.service.PoliceStationService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.ResponsePage;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@GraphQLApi
public class PoliceStationController {
    private final PoliceStationService policeStationService;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final UserService userService;
    private final PoliceStationRepository policeStationRepository;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLMutation(name = "savePoliceStation", description = "New police station is registered or update old one")
    public Response<PoliceStation> savePoliceStation(@GraphQLArgument(name = "policeStationDto") PoliceStationDto policeStationDto) {
        return policeStationService.savePoliceStation(policeStationDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLQuery(name = "getPoliceStation", description = "Gets a police station using its uid")
    public Response<PoliceStation> getPoliceStation(@GraphQLArgument(name = "uid") String uid) {
        return policeStationService.getPoliceStation(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @GraphQLMutation(name = "deletePoliceStation", description = "Deletes a police station using its uid")
    public Response<PoliceStation> deletePoliceStation(@GraphQLArgument(name = "uid") String uid) {
        return policeStationService.deletePoliceStation(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLQuery(name = "getPoliceStations", description = "Gets a page of police stations")
    public ResponsePage<PoliceStation> getPoliceStations(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam) {
        return policeStationService.getPoliceStations(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLQuery(name = "getStationsByAdmin")
    public ResponseList<PoliceStationDto> getStationsByAdmin() {
        String phone = jwtAuthInterceptor.extractPhoneFromRequest();
        if (phone == null) {
            return ResponseList.error("Invalid or missing authentication token");
        }

        Response<User> userResponse = userService.getUserByPhone(phone);
        User user = userResponse.getData();
        if (user == null) {
            return ResponseList.error("User not found for the provided phone number");
        }

        List<PoliceStation> stations;
        if (user.getRole() == Role.ROOT) {
            stations = getAllStations().getData();
        } else {
            stations = user.getStation() != null ? List.of(user.getStation()) : List.of();
        }

        List<PoliceStationDto> stationDtos = stations.stream().map(station -> {
            PoliceStationDto dto = new PoliceStationDto();
            dto.setUid(station.getUid());
            dto.setName(station.getName());
            dto.setContactInfo(station.getContactInfo());
            if (station.getLocation() != null) {
                dto.setLocation(new LocationDto(
                        station.getLocation().getLatitude(),
                        station.getLocation().getLongitude(),
                        station.getLocation().getAddress()
                ));
            }
            return dto;
        }).toList();

        return new ResponseList<>(stationDtos);
    }

    public ResponseList<PoliceStation> getAllStations() {
        List<PoliceStation> stations = policeStationRepository.findAll();
        return new ResponseList<>(stations);
    }

    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT,Role.POLICE_OFFICER,Role.CITIZEN})
    @GraphQLQuery(name = "getNearbyPoliceStations", description = "Gets police stations near a given location")
    public ResponseList<PoliceStation> getNearbyPoliceStations(
            @GraphQLArgument(name = "latitude") double latitude,
            @GraphQLArgument(name = "longitude") double longitude,
            @GraphQLArgument(name = "maxDistance") double maxDistance) {
        return policeStationService.getNearbyPoliceStations(latitude, longitude, maxDistance);
    }
}