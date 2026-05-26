package com.smartincident.incidentbackend.emergency.service;

import com.smartincident.incidentbackend.emergency.dto.EmergencyUnitDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyUnitService {

    /** Police unit types are managed via PoliceStation — block creation here. */
    private static final Set<UnitType> POLICE_TYPES = Set.of(
            UnitType.POLICE_POST, UnitType.POLICE_STATION,
            UnitType.DISTRICT_POLICE_HQ, UnitType.REGIONAL_POLICE_HQ,
            UnitType.ZONAL_POLICE_HQ, UnitType.NATIONAL_POLICE_HQ
    );

    private final EmergencyUnitRepository unitRepository;
    private final AgencyRepository agencyRepository;
    private final AdministrativeAreaRepository areaRepository;

    // ── Save (create or update) ────────────────────────────────────────────

    @Transactional
    public Response<EmergencyUnit> saveUnit(EmergencyUnitDto dto) {
        if (dto == null) return Response.error("Unit data is required");
        if (dto.getName() == null || dto.getName().trim().isEmpty())
            return Response.error("Unit name is required");
        if (dto.getUnitType() == null) return Response.error("Unit type is required");
        if (POLICE_TYPES.contains(dto.getUnitType()))
            return Response.error("Police units are managed via the Police Station API");
        if (dto.getLevel() == null) return Response.error("Unit level is required");

        EmergencyUnit unit;

        if (dto.getUid() != null) {
            // UPDATE
            Optional<EmergencyUnit> existing = unitRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Emergency unit not found");
            unit = existing.get();
            unit.setName(dto.getName().trim());
            if (dto.getUnitType() != null && !POLICE_TYPES.contains(dto.getUnitType()))
                unit.setUnitType(dto.getUnitType());
            if (dto.getLevel() != null) unit.setLevel(dto.getLevel());
            if (dto.getContactInfo() != null) unit.setContactInfo(dto.getContactInfo());
            unit.update();
        } else {
            // CREATE
            if (dto.getAgencyUid() == null) return Response.error("Agency is required");
            Agency agency = agencyRepository.findByCode(dto.getAgencyUid())
                    .orElseGet(() -> agencyRepository.findByUid(dto.getAgencyUid()).orElse(null));
            if (agency == null) return Response.error("Agency not found");

            unit = EmergencyUnit.builder()
                    .name(dto.getName().trim())
                    .agency(agency)
                    .unitType(dto.getUnitType())
                    .level(dto.getLevel())
                    .contactInfo(dto.getContactInfo())
                    .build();
        }

        // Location
        if (dto.getLocation() != null) {
            unit.setLocation(new Location(
                    dto.getLocation().getLatitude(),
                    dto.getLocation().getLongitude(),
                    dto.getLocation().getAddress()));
        }

        // Parent unit
        if (dto.getParentUnitUid() != null) {
            unitRepository.findByUid(dto.getParentUnitUid())
                    .ifPresent(unit::setParentUnit);
        }

        // Administrative area
        if (dto.getAdministrativeAreaUid() != null) {
            areaRepository.findByUid(dto.getAdministrativeAreaUid())
                    .ifPresent(unit::setAdministrativeArea);
        }

        try {
            EmergencyUnit saved = unitRepository.save(unit);
            log.info("Emergency unit saved: {} ({})", saved.getName(), saved.getUnitType());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save emergency unit: {}", e.getMessage());
            return Response.error("Failed to save emergency unit: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public Response<EmergencyUnit> getUnit(String uid) {
        if (uid == null) return Response.error("UID is required");
        return unitRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("Emergency unit not found"));
    }

    public ResponsePage<EmergencyUnit> getUnits(PageableParam param,
                                                 String agencyUid,
                                                 UnitType unitType,
                                                 UnitLevel level) {
        Page<EmergencyUnit> page = unitRepository.search(
                param.getPageable(false), param.key(), param.getIsActive(),
                agencyUid, unitType, level);
        return new ResponsePage<>(page);
    }

    public ResponseList<EmergencyUnit> getByAgency(String agencyUid) {
        List<EmergencyUnit> units = unitRepository.findByAgencyUidAndIsActiveTrue(agencyUid);
        return new ResponseList<>(units);
    }

    public ResponseList<EmergencyUnit> getChildren(String parentUid) {
        List<EmergencyUnit> units = unitRepository.findByParentUnitUidAndIsActiveTrue(parentUid);
        return new ResponseList<>(units);
    }

    public ResponseList<EmergencyUnit> getNearby(double lat, double lng, double radiusKm, UnitType unitType) {
        List<EmergencyUnit> candidates = unitType != null
                ? unitRepository.findByUnitTypeAndIsActiveTrue(unitType)
                : unitRepository.findAll().stream().filter(EmergencyUnit::getIsActive).toList();

        List<EmergencyUnit> nearby = candidates.stream()
                .filter(u -> u.getLocation() != null
                        && u.getLocation().getLatitude() != null
                        && u.getLocation().getLongitude() != null)
                .peek(u -> u.setTemporaryDistance(haversine(lat, lng,
                        u.getLocation().getLatitude(), u.getLocation().getLongitude())))
                .filter(u -> u.getTemporaryDistance() <= radiusKm)
                .sorted(Comparator.comparingDouble(EmergencyUnit::getTemporaryDistance))
                .toList();

        return new ResponseList<>(nearby);
    }

    // ── Delete (soft) ──────────────────────────────────────────────────────

    @Transactional
    public Response<EmergencyUnit> deleteUnit(String uid) {
        if (uid == null) return Response.error("UID is required");
        Optional<EmergencyUnit> opt = unitRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Emergency unit not found");
        EmergencyUnit unit = opt.get();
        if (!unit.getIsActive()) return Response.error("Unit already deleted");
        unit.delete();
        try {
            unitRepository.save(unit);
            log.info("Emergency unit deleted: {}", uid);
            return Response.success(unit);
        } catch (Exception e) {
            return Response.error("Failed to delete unit: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
