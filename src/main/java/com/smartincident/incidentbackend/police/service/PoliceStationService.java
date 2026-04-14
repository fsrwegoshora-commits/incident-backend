package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.StationLevel;
import com.smartincident.incidentbackend.police.dto.PoliceStationDto;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@GraphQLApi
public class PoliceStationService {

    private final PoliceStationRepository policeStationRepository;
    private final AdministrativeAreaRepository administrativeAreaRepository;

    public Response<PoliceStation> savePoliceStation(PoliceStationDto dto) {
        if (dto == null)
            return Response.error("Police station is required");
        if (dto.getName() == null)
            return Response.error("Police station name is required");
        if (dto.getContactInfo() == null)
            return Response.error("Contact information is required");
        if (dto.getAdministrativeAreaUid() == null)
            return Response.error("Administrative area is required");
        if (dto.getLocation() == null)
            return Response.error("Location is required");
        if (dto.getLocation().getLatitude() == null || dto.getLocation().getLongitude() == null)
            return Response.error("Latitude and longitude are required");

        StationLevel level = dto.getLevel() != null ? dto.getLevel() : StationLevel.POLICE_STATION;

        // Every level except NATIONAL_HQ must have a parent
        if (level != StationLevel.NATIONAL_HQ && dto.getParentStationUid() == null)
            return Response.error("Parent station is required for all station levels except National HQ");

        PoliceStation policeStation;

        if (dto.getUid() != null) {
            Optional<PoliceStation> oPoliceStation = policeStationRepository.findByUid(dto.getUid());
            if (oPoliceStation.isEmpty())
                return Response.error("Invalid police station provided");

            policeStation = oPoliceStation.get();
            policeStation.setName(dto.getName());
            policeStation.setContactInfo(dto.getContactInfo());
            policeStation.setLevel(level);

            AdministrativeArea area = administrativeAreaRepository.findByUid(dto.getAdministrativeAreaUid())
                    .orElseThrow(() -> new RuntimeException("Invalid administrative area"));
            policeStation.setPoliceStationLocation(area);

            policeStation.setLocation(new Location(
                    dto.getLocation().getLatitude(),
                    dto.getLocation().getLongitude(),
                    dto.getLocation().getAddress()));

            policeStation.setParentStation(resolveParent(dto.getParentStationUid()));
            policeStation.update();
        } else {
            if (policeStationRepository.findByNameAndIsActiveTrue(dto.getName()).isPresent())
                return Response.error("Police station with this name is already registered");

            policeStation = new PoliceStation();
            policeStation.setName(dto.getName());
            policeStation.setContactInfo(dto.getContactInfo());
            policeStation.setLevel(level);

            AdministrativeArea area = administrativeAreaRepository.findByUid(dto.getAdministrativeAreaUid())
                    .orElseThrow(() -> new RuntimeException("Invalid administrative area"));
            policeStation.setPoliceStationLocation(area);

            policeStation.setLocation(new Location(
                    dto.getLocation().getLatitude(),
                    dto.getLocation().getLongitude(),
                    dto.getLocation().getAddress()));

            policeStation.setParentStation(resolveParent(dto.getParentStationUid()));
        }

        try {
            policeStation = policeStationRepository.save(policeStation);
            log.info("Police station saved successfully: {}", policeStation.getName());
        } catch (Exception e) {
            log.error("Failed to save police station: {}", e.getMessage());
            return Response.error("Failed to save police station: " + e.getMessage());
        }

        return new Response<>(policeStation);
    }

    private PoliceStation resolveParent(String parentUid) {
        if (parentUid == null) return null;
        return policeStationRepository.findByUid(parentUid)
                .orElseThrow(() -> new RuntimeException("Parent station not found: " + parentUid));
    }

    public Response<PoliceStation> getPoliceStation(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<PoliceStation> optionalPoliceStation = policeStationRepository.findByUid(uid);
        return optionalPoliceStation.map(Response::new).orElseGet(() -> new Response<>("Invalid police station provided"));
    }

    public Response<PoliceStation> deletePoliceStation(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<PoliceStation> optionalPoliceStation = policeStationRepository.findByUid(uid);
        if (optionalPoliceStation.isEmpty())
            return new Response<>("Invalid police station provided");
        if (!optionalPoliceStation.get().getIsActive())
            return new Response<>("police station already deleted");
        optionalPoliceStation.get().delete();
        PoliceStation policeStation = optionalPoliceStation.get();
        try {
            policeStationRepository.save(policeStation);
            log.info("police station deleted successfully: {}", policeStation.getName());
        } catch (Exception e) {
            log.error("Failed to delete police station: {}", e.getMessage());
            String message = Utils.getExceptionMessage(e);
            return new Response<>(message);
        }
        return Response.success(policeStation);
    }

    public ResponsePage<PoliceStation> getPoliceStations(PageableParam pageableParam) {
        String stationUid = LoggedUser.getStationUid();
        return new ResponsePage<>(policeStationRepository.getPoliceStations(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(),stationUid));
    }

    public ResponseList<PoliceStation> getNearbyPoliceStations(double latitude, double longitude, double maxDistance) {
        List<PoliceStation> allStations = policeStationRepository.findByIsActiveTrue();

        List<PoliceStation> nearbyStations = allStations.stream()
                .filter(station -> station.getLocation() != null)
                .peek(station -> {
                    double dist = calculateDistance(latitude, longitude,
                            station.getLocation().getLatitude(), station.getLocation().getLongitude());
                    station.setTemporaryDistance(dist);
                })
                .filter(station -> station.getTemporaryDistance() <= maxDistance)
                .sorted(Comparator.comparingDouble(PoliceStation::getTemporaryDistance))
                .collect(Collectors.toList());

        // construct proper ResponseList
        ResponseList<PoliceStation> response = new ResponseList<>();
        response.setData(nearbyStations);
        response.setMessage("Nearby police stations found");
        return response;
    }


    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2 - lat1);
        double dLon = deg2rad(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }

    private double deg2rad(double deg) {
        return deg * (Math.PI / 180);
    }
}