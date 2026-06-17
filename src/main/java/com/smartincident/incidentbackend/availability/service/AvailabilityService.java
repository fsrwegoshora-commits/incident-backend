package com.smartincident.incidentbackend.availability.service;

import com.smartincident.incidentbackend.availability.dto.ForceAvailabilityDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.emergency.repository.VehicleShiftRepository;
import com.smartincident.incidentbackend.enums.PostStatus;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.operational.repository.OperationalPostRepository;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final EmergencyVehicleRepository vehicleRepository;
    private final VehicleShiftRepository vehicleShiftRepository;
    private final EmergencyUnitRepository unitRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final OperationalPostRepository postRepository;
    public Response<ForceAvailabilityDto> getForceAvailability() {
        LocalDate today = LocalDate.now();

        // All active vehicles
        List<EmergencyVehicle> allVehicles = vehicleRepository
                .findVehicles(PageRequest.of(0, 10000), true, "", null)
                .getContent();

        Map<VehicleStatus, Long> byStatus = allVehicles.stream()
                .collect(Collectors.groupingBy(EmergencyVehicle::getStatus, Collectors.counting()));

        Map<String, Long> byStatusStr = new LinkedHashMap<>();
        for (VehicleStatus s : VehicleStatus.values()) {
            byStatusStr.put(s.name(), byStatus.getOrDefault(s, 0L));
        }

        // Officer shifts today
        List<OfficerShift> officerShiftsToday = officerShiftRepository.findByDate(today);
        long officersOnDuty = allVehicles.stream()
                .filter(v -> v.getStatus() != VehicleStatus.AVAILABLE && v.getStatus() != VehicleStatus.OUT_OF_SERVICE)
                .count();

        // Vehicle shifts active today
        List<VehicleShift> vehicleShiftsToday = vehicleShiftRepository.findActiveByDate(today);

        // Active operational posts
        long activePosts = postRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getStatus() == PostStatus.ACTIVE)
                .count();

        // Per-unit breakdown
        List<EmergencyUnit> units = unitRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .collect(Collectors.toList());

        List<ForceAvailabilityDto.UnitSummary> unitSummaries = new ArrayList<>();

        for (EmergencyUnit unit : units) {
            long totalVehicles = allVehicles.stream()
                    .filter(v -> v.getEmergencyUnit() != null && unit.getUid().equals(v.getEmergencyUnit().getUid()))
                    .count();
            long availableVehicles = allVehicles.stream()
                    .filter(v -> v.getEmergencyUnit() != null
                            && unit.getUid().equals(v.getEmergencyUnit().getUid())
                            && v.getStatus() == VehicleStatus.AVAILABLE)
                    .count();
            long shiftCount = vehicleShiftsToday.stream()
                    .filter(s -> s.getVehicle() != null && s.getVehicle().getEmergencyUnit() != null
                            && unit.getUid().equals(s.getVehicle().getEmergencyUnit().getUid()))
                    .count();

            if (totalVehicles == 0 && shiftCount == 0) continue;

            unitSummaries.add(ForceAvailabilityDto.UnitSummary.builder()
                    .uid(unit.getUid())
                    .name(unit.getName())
                    .type(unit.getUnitType() != null ? unit.getUnitType().name() : null)
                    .vehiclesTotal(totalVehicles)
                    .vehiclesAvailable(availableVehicles)
                    .officersOnDuty(0)
                    .activeShifts(shiftCount)
                    .build());
        }

        ForceAvailabilityDto dto = ForceAvailabilityDto.builder()
                .vehiclesTotal(allVehicles.size())
                .vehiclesAvailable(byStatus.getOrDefault(VehicleStatus.AVAILABLE, 0L))
                .vehiclesDispatched(byStatus.getOrDefault(VehicleStatus.DISPATCHED, 0L))
                .vehiclesOnScene(byStatus.getOrDefault(VehicleStatus.ON_SCENE, 0L))
                .vehiclesReturning(byStatus.getOrDefault(VehicleStatus.RETURNING, 0L))
                .vehiclesMaintenance(byStatus.getOrDefault(VehicleStatus.MAINTENANCE, 0L))
                .vehiclesByStatus(byStatusStr)
                .officersOnShiftToday((long) officerShiftsToday.size())
                .officersOnDuty(officersOnDuty)
                .vehicleShiftsActive((long) vehicleShiftsToday.size())
                .operationalPostsActive(activePosts)
                .units(unitSummaries)
                .build();

        return Response.success(dto);
    }
}
