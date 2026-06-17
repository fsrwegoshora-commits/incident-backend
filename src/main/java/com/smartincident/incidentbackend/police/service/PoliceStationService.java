package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.enums.StationLevel;
import com.smartincident.incidentbackend.police.dto.PoliceStationDto;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
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
    private final AgencyRepository agencyRepository;

    public Response<PoliceStation> savePoliceStation(PoliceStationDto dto) {
        if (dto == null)
            return Response.error("Police station is required");
        if (dto.getName() == null)
            return Response.error("Police station name is required");
        if (dto.getContactInfo() == null)
            return Response.error("Contact information is required");
        if (dto.getLocation() == null)
            return Response.error("Location is required");
        if (dto.getLocation().getLatitude() == null || dto.getLocation().getLongitude() == null)
            return Response.error("Latitude and longitude are required");

        StationLevel level = dto.getLevel() != null ? dto.getLevel() : StationLevel.POLICE_STATION;

        // Resolve owning agency
        Agency agency = resolveAgency(dto.getAgencyUid());
        if (dto.getUid() == null && agency == null)
            return Response.error("Agency assignment is required. Specify agencyUid or log in as Agency Administrator.");

        PoliceStation policeStation;

        if (dto.getUid() != null) {
            Optional<PoliceStation> oPoliceStation = policeStationRepository.findByUid(dto.getUid());
            if (oPoliceStation.isEmpty())
                return Response.error("Invalid police station provided");

            policeStation = oPoliceStation.get();

            // AGENCY_ADMIN can only update stations belonging to their agency
            if (LoggedUser.isAgencyAdmin()) {
                String adminAgencyUid = LoggedUser.getAgencyUid();
                String stationAgencyUid = policeStation.getAgency() != null ? policeStation.getAgency().getUid() : null;
                if (adminAgencyUid == null || !adminAgencyUid.equals(stationAgencyUid))
                    return Response.error("You are not authorized to update this station");
            }

            policeStation.setName(dto.getName());
            policeStation.setContactInfo(dto.getContactInfo());
            policeStation.setLevel(level);
            // Agency only updated by ROOT when explicitly provided
            if (LoggedUser.isRoot() && agency != null)
                policeStation.setAgency(agency);

            if (dto.getAdministrativeAreaUid() != null) {
                administrativeAreaRepository.findByUid(dto.getAdministrativeAreaUid())
                        .ifPresent(policeStation::setPoliceStationLocation);
            }

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
            policeStation.setAgency(agency);

            if (dto.getAdministrativeAreaUid() != null) {
                administrativeAreaRepository.findByUid(dto.getAdministrativeAreaUid())
                        .ifPresent(policeStation::setPoliceStationLocation);
            }

            policeStation.setLocation(new Location(
                    dto.getLocation().getLatitude(),
                    dto.getLocation().getLongitude(),
                    dto.getLocation().getAddress()));

            policeStation.setParentStation(resolveParent(dto.getParentStationUid()));
        }

        try {
            policeStation = policeStationRepository.save(policeStation);
            log.info("Police station '{}' saved under agency '{}'", policeStation.getName(),
                policeStation.getAgency() != null ? policeStation.getAgency().getName() : "none");
        } catch (Exception e) {
            log.error("Failed to save police station: {}", e.getMessage());
            return Response.error("Failed to save police station: " + e.getMessage());
        }

        return new Response<>(policeStation);
    }

    /**
     * Resolves the agency to assign to a station.
     * AGENCY_ADMIN: always uses their own agency (ignores dto value).
     * ROOT: uses the agencyUid from the DTO if provided.
     */
    private Agency resolveAgency(String dtoAgencyUid) {
        if (LoggedUser.isAgencyAdmin()) {
            String agencyUid = LoggedUser.getAgencyUid();
            if (agencyUid == null) return null;
            return agencyRepository.findByUid(agencyUid).orElse(null);
        }
        if (dtoAgencyUid != null) {
            return agencyRepository.findByUid(dtoAgencyUid).orElse(null);
        }
        return null;
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

        // AGENCY_ADMIN can only delete stations belonging to their agency
        if (LoggedUser.isAgencyAdmin()) {
            String adminAgencyUid = LoggedUser.getAgencyUid();
            String stationAgencyUid = optionalPoliceStation.get().getAgency() != null
                ? optionalPoliceStation.get().getAgency().getUid() : null;
            if (adminAgencyUid == null || !adminAgencyUid.equals(stationAgencyUid))
                return Response.error("You are not authorized to delete this station");
        }

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

    public ResponsePage<PoliceStation> getPoliceStations(PageableParam pageableParam, String filterAgencyUid) {
        Role role = LoggedUser.getRole();

        if (role == Role.AGENCY_ADMIN) {
            // AGENCY_ADMIN sees stations assigned to their agency
            // AND stations not yet assigned to any agency (so orphan stations are claimable).
            String agencyUid = LoggedUser.getAgencyUid();
            if (agencyUid == null) {
                // No agency linked to this admin — return all active stations as safe fallback
                return new ResponsePage<>(policeStationRepository.getPoliceStations(
                    pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), null, null));
            }
            return new ResponsePage<>(policeStationRepository.getPoliceStationsForAgencyAdmin(
                pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), agencyUid));
        }

        String stationUid = null;
        String agencyUid  = null;

        if (role == Role.STATION_ADMIN) {
            stationUid = LoggedUser.getPoliceStationUid();
        } else if (role == Role.ROOT && filterAgencyUid != null) {
            agencyUid = filterAgencyUid;
        }
        // DISPATCHER_SUPERVISOR, DISPATCHER, DISPATCH_CENTER_ADMIN, ROOT (no filter):
        // stationUid and agencyUid remain null → query returns all active stations

        return new ResponsePage<>(policeStationRepository.getPoliceStations(
            pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), stationUid, agencyUid));
    }

    public ResponseList<PoliceStation> getNearbyPoliceStations(double latitude, double longitude, double maxDistance) {
        Role role = LoggedUser.getRole();
        List<PoliceStation> allStations;
        if (role == Role.AGENCY_ADMIN) {
            String agencyUid = LoggedUser.getAgencyUid();
            allStations = agencyUid != null
                    ? policeStationRepository.findByAgencyUidAndIsActiveTrue(agencyUid)
                    : List.of();
        } else {
            allStations = policeStationRepository.findByIsActiveTrue();
        }

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