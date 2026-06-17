package com.smartincident.incidentbackend.gis.service;

import com.smartincident.incidentbackend.enums.GeofenceType;
import com.smartincident.incidentbackend.gis.dto.GeofenceDto;
import com.smartincident.incidentbackend.gis.entity.Geofence;
import com.smartincident.incidentbackend.gis.repository.GeofenceRepository;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;

    public ResponseList<Geofence> getAll(String agencyUid, GeofenceType type) {
        List<Geofence> result;
        if (type != null) {
            result = geofenceRepository.findByTypeAndIsActiveTrue(type);
        } else if (agencyUid != null && !agencyUid.isBlank()) {
            result = geofenceRepository.findByAgencyUidAndIsActiveTrue(agencyUid);
        } else {
            result = geofenceRepository.findByIsActiveTrueOrderByNameAsc();
        }
        return new ResponseList<>(result);
    }

    public Response<Geofence> getByUid(String uid) {
        return geofenceRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("Geofence not found"));
    }

    @Transactional
    public Response<Geofence> create(GeofenceDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            return Response.error("Name is required");
        if (dto.getType() == null)
            return Response.error("Type is required");
        if (dto.getCenterLatitude() == null || dto.getCenterLongitude() == null)
            return Response.error("Center coordinates are required");
        if (dto.getRadiusMetres() == null || dto.getRadiusMetres() <= 0)
            return Response.error("Radius must be a positive number");

        Geofence geofence = Geofence.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .type(dto.getType())
                .centerLatitude(dto.getCenterLatitude())
                .centerLongitude(dto.getCenterLongitude())
                .radiusMetres(dto.getRadiusMetres())
                .agencyUid(dto.getAgencyUid())
                .alertOnEntry(dto.getAlertOnEntry() != null ? dto.getAlertOnEntry() : true)
                .alertOnExit(dto.getAlertOnExit() != null ? dto.getAlertOnExit() : false)
                .build();

        try {
            return Response.success(geofenceRepository.save(geofence));
        } catch (Exception e) {
            log.error("Failed to create geofence: {}", e.getMessage());
            return Response.error("Failed to create geofence");
        }
    }

    @Transactional
    public Response<Geofence> update(String uid, GeofenceDto dto) {
        Optional<Geofence> opt = geofenceRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Geofence not found");
        Geofence geofence = opt.get();

        if (dto.getName() != null)            geofence.setName(dto.getName().trim());
        if (dto.getDescription() != null)     geofence.setDescription(dto.getDescription());
        if (dto.getType() != null)            geofence.setType(dto.getType());
        if (dto.getCenterLatitude() != null)  geofence.setCenterLatitude(dto.getCenterLatitude());
        if (dto.getCenterLongitude() != null) geofence.setCenterLongitude(dto.getCenterLongitude());
        if (dto.getRadiusMetres() != null)    geofence.setRadiusMetres(dto.getRadiusMetres());
        if (dto.getAgencyUid() != null)       geofence.setAgencyUid(dto.getAgencyUid());
        if (dto.getAlertOnEntry() != null)    geofence.setAlertOnEntry(dto.getAlertOnEntry());
        if (dto.getAlertOnExit() != null)     geofence.setAlertOnExit(dto.getAlertOnExit());
        geofence.update();

        try {
            return Response.success(geofenceRepository.save(geofence));
        } catch (Exception e) {
            log.error("Failed to update geofence: {}", e.getMessage());
            return Response.error("Failed to update geofence");
        }
    }

    @Transactional
    public Response<Geofence> delete(String uid) {
        Optional<Geofence> opt = geofenceRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Geofence not found");
        Geofence geofence = opt.get();
        geofence.delete();
        geofenceRepository.save(geofence);
        return Response.success(geofence);
    }

    /**
     * Check which geofences contain the given coordinates.
     * Used to generate entry/exit alerts when a vehicle updates its GPS position.
     */
    public ResponseList<Geofence> checkPoint(double lat, double lng) {
        return new ResponseList<>(geofenceRepository.findContainingPoint(lat, lng));
    }
}
