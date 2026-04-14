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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/police/stations")
@RequiredArgsConstructor
public class PoliceStationController {
    private final PoliceStationService policeStationService;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final UserService userService;
    private final PoliceStationRepository policeStationRepository;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PostMapping
    public Response<PoliceStation> savePoliceStation(@RequestBody PoliceStationDto policeStationDto) {
        return policeStationService.savePoliceStation(policeStationDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/{uid}")
    public Response<PoliceStation> getPoliceStation(@PathVariable String uid) {
        return policeStationService.getPoliceStation(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<PoliceStation> deletePoliceStation(@PathVariable String uid) {
        return policeStationService.deletePoliceStation(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping
    public ResponsePage<PoliceStation> getPoliceStations(@ModelAttribute PageableParam pageableParam) {
        return policeStationService.getPoliceStations(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/admin")
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

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.POLICE_OFFICER, Role.CITIZEN})
    @GetMapping("/nearby")
    public ResponseList<PoliceStation> getNearbyPoliceStations(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double maxDistance) {
        return policeStationService.getNearbyPoliceStations(latitude, longitude, maxDistance);
    }
}
