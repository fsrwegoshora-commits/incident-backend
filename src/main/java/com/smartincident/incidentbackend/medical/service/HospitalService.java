package com.smartincident.incidentbackend.medical.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.HospitalType;
import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.medical.dto.HospitalDto;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.repository.HospitalRepository;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final AgencyRepository agencyRepository;

    // ── Save ───────────────────────────────────────────────────────────────

    @Transactional
    public Response<Hospital> saveHospital(HospitalDto dto) {
        if (dto == null) return Response.error("Hospital data is required");
        if (dto.getName() == null || dto.getName().trim().isEmpty())
            return Response.error("Hospital name is required");
        if (dto.getHospitalType() == null) return Response.error("Hospital type is required");

        Hospital hospital;

        if (dto.getUid() != null) {
            // UPDATE
            Optional<Hospital> opt = hospitalRepository.findByUid(dto.getUid());
            if (opt.isEmpty()) return Response.error("Hospital not found");
            hospital = opt.get();
            hospital.setName(dto.getName().trim());
            if (dto.getHospitalType() != null) hospital.setHospitalType(dto.getHospitalType());
            if (dto.getBedCapacity() != null) hospital.setBedCapacity(dto.getBedCapacity());
            if (dto.getEmergencyContact() != null) hospital.setEmergencyContact(dto.getEmergencyContact());
            hospital.update();
        } else {
            // CREATE
            if (dto.getLocation() == null || dto.getLocation().getLatitude() == null)
                return Response.error("Hospital location with GPS coordinates is required");

            hospital = Hospital.builder()
                    .name(dto.getName().trim())
                    .hospitalType(dto.getHospitalType())
                    .bedCapacity(dto.getBedCapacity())
                    .emergencyContact(dto.getEmergencyContact())
                    .build();
        }

        if (dto.getLocation() != null) {
            hospital.setLocation(new Location(
                    dto.getLocation().getLatitude(),
                    dto.getLocation().getLongitude(),
                    dto.getLocation().getAddress()));
        }

        if (dto.getAmbulanceUnitUid() != null) {
            Optional<EmergencyUnit> unitOpt = emergencyUnitRepository.findByUid(dto.getAmbulanceUnitUid());
            if (unitOpt.isEmpty()) return Response.error("Ambulance unit not found");
            hospital.setAmbulanceUnit(unitOpt.get());
        } else if (hospital.getAmbulanceUnit() == null) {
            // Auto-create a HOSPITAL_AMBULANCE_UNIT so the hospital is immediately operational
            Optional<Agency> agencyOpt = agencyRepository.findByCode("AMBULANCE");
            if (agencyOpt.isPresent()) {
                EmergencyUnit unit = EmergencyUnit.builder()
                        .name(dto.getName().trim() + " Ambulance Unit")
                        .agency(agencyOpt.get())
                        .unitType(UnitType.HOSPITAL_AMBULANCE_UNIT)
                        .level(UnitLevel.HOSPITAL_BASED)
                        .build();
                unit = emergencyUnitRepository.save(unit);
                hospital.setAmbulanceUnit(unit);
                log.info("Auto-created ambulance unit for hospital: {}", dto.getName());
            } else {
                log.warn("AMBULANCE agency not found — hospital will have no ambulance unit");
            }
        }

        try {
            Hospital saved = hospitalRepository.save(hospital);
            log.info("Hospital saved: {}", saved.getName());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save hospital: {}", e.getMessage());
            return Response.error("Failed to save hospital: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public Response<Hospital> getHospital(String uid) {
        if (uid == null) return Response.error("UID is required");
        return hospitalRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("Hospital not found"));
    }

    public ResponsePage<Hospital> getHospitals(PageableParam param, HospitalType hospitalType) {
        return new ResponsePage<>(hospitalRepository.search(
                param.getPageable(false), param.key(), param.getIsActive(), hospitalType));
    }

    public ResponseList<Hospital> getNearby(double lat, double lng, double radiusKm) {
        List<Hospital> all = hospitalRepository.findAll().stream()
                .filter(Hospital::getIsActive)
                .filter(h -> h.getLocation() != null && h.getLocation().getLatitude() != null)
                .toList();

        List<Hospital> nearby = all.stream()
                .filter(h -> haversine(lat, lng,
                        h.getLocation().getLatitude(),
                        h.getLocation().getLongitude()) <= radiusKm)
                .sorted(Comparator.comparingDouble(h -> haversine(lat, lng,
                        h.getLocation().getLatitude(), h.getLocation().getLongitude())))
                .toList();

        return new ResponseList<>(nearby);
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    @Transactional
    public Response<Hospital> deleteHospital(String uid) {
        if (uid == null) return Response.error("UID is required");
        Optional<Hospital> opt = hospitalRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Hospital not found");
        Hospital hospital = opt.get();
        if (!hospital.getIsActive()) return Response.error("Hospital already deleted");
        hospital.delete();
        try {
            hospitalRepository.save(hospital);
            log.info("Hospital deleted: {}", uid);
            return Response.success(hospital);
        } catch (Exception e) {
            return Response.error("Failed to delete hospital: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────

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
