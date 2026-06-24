package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.dispatcher.dto.DispatchCenterPerformanceDto;
import com.smartincident.incidentbackend.dispatcher.dto.DispatcherShiftDto;
import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.dispatcher.service.DispatcherShiftService;
import com.smartincident.incidentbackend.emergency.dto.EmergencyUnitDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.service.EmergencyUnitService;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dispatch-centers")
@RequiredArgsConstructor
@Slf4j
public class DispatchCenterController {

    private final EmergencyUnitService unitService;
    private final UserRepository userRepository;
    private final DispatcherShiftService shiftService;
    private final DispatcherShiftRepository shiftRepository;
    private final IncidentReportRepository incidentRepository;

    /** Create or update a Dispatch Center. ROOT / AGENCY_ADMIN only. */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_AGENCY)
    @PostMapping
    public Response<EmergencyUnit> save(@RequestBody EmergencyUnitDto dto) {
        log.info("Saving dispatch center: {}", dto.getName());
        dto.setUnitType(UnitType.DISPATCH_CENTER);
        return unitService.saveUnit(dto);
    }

    /** List all Dispatch Centers. */
    @Authenticated
    @GetMapping
    public ResponsePage<EmergencyUnit> list(@ModelAttribute PageableParam param) {
        return unitService.getUnits(
                param != null ? param : new PageableParam(),
                null, UnitType.DISPATCH_CENTER, null);
    }

    /** Get a single Dispatch Center by UID. */
    @Authenticated
    @GetMapping("/{uid}")
    public Response<EmergencyUnit> get(@PathVariable String uid) {
        return unitService.getUnit(uid);
    }

    /** List all Dispatcher users belonging to a Dispatch Center. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @GetMapping("/{uid}/dispatchers")
    public ResponseList<User> getDispatchers(@PathVariable String uid) {
        log.info("Fetching dispatchers for center: {}", uid);
        List<User> dispatchers = userRepository.findByRoleInAndEmergencyUnitUidAndIsActiveTrue(
                List.of(Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR), uid);
        return new ResponseList<>(dispatchers);
    }

    /** List all shifts for dispatchers in a Dispatch Center. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @GetMapping("/{uid}/shifts")
    public ResponseList<DispatcherShift> getShifts(@PathVariable String uid) {
        log.info("Fetching dispatcher shifts for center: {}", uid);
        return shiftService.getShiftsByDispatchCenter(uid);
    }

    /** Create a dispatcher shift — available to DISPATCH_CENTER_ADMIN and above. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @PostMapping("/{uid}/shifts")
    public Response<DispatcherShift> createShift(@PathVariable String uid,
                                                  @RequestBody DispatcherShiftDto dto) {
        log.info("Creating dispatcher shift for center: {}", uid);
        return shiftService.saveShift(dto);
    }

    /** List all supervisors (DISPATCHER_SUPERVISOR role) in a Dispatch Center. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @GetMapping("/{uid}/supervisors")
    public ResponseList<User> getSupervisors(@PathVariable String uid) {
        log.info("Fetching supervisors for center: {}", uid);
        List<User> supervisors = userRepository.findByRoleInAndEmergencyUnitUidAndIsActiveTrue(
                List.of(Role.DISPATCHER_SUPERVISOR), uid);
        return new ResponseList<>(supervisors);
    }

    /** Performance metrics for a Dispatch Center. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @GetMapping("/{uid}/performance")
    public Response<DispatchCenterPerformanceDto> getPerformance(@PathVariable String uid) {
        log.info("Fetching performance for center: {}", uid);

        Response<EmergencyUnit> centerResp = unitService.getUnit(uid);
        if (!centerResp.success() || centerResp.getData() == null)
            return Response.error("Dispatch center not found");
        EmergencyUnit center = centerResp.getData();

        List<User> dispatchers = userRepository.findByRoleInAndEmergencyUnitUidAndIsActiveTrue(
                List.of(Role.DISPATCHER), uid);
        List<User> supervisors = userRepository.findByRoleInAndEmergencyUnitUidAndIsActiveTrue(
                List.of(Role.DISPATCHER_SUPERVISOR), uid);
        List<User> allStaff = new java.util.ArrayList<>(dispatchers);
        allStaff.addAll(supervisors);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<DispatcherShift> onDutyShifts = shiftRepository.findOnDutyNow(today, now);
        long onDutyCount = onDutyShifts.stream()
                .filter(s -> allStaff.stream().anyMatch(u -> u.getUid().equals(s.getDispatcher().getUid())))
                .count();

        List<DispatchCenterPerformanceDto.DispatcherMetric> metrics = allStaff.stream().map(user -> {
            long active = incidentRepository.countActiveIncidentsByDispatcher(user.getUid());
            long resolvedToday = incidentRepository.findAll().stream()
                    .filter(i -> user.getUid().equals(i.getAssignedDispatcher() != null ? i.getAssignedDispatcher().getUid() : null)
                            && i.getStatus() == IncidentStatus.RESOLVED
                            && i.getResolvedAt() != null
                            && i.getResolvedAt().toLocalDate().equals(today))
                    .count();
            boolean onDuty = onDutyShifts.stream()
                    .anyMatch(s -> s.getDispatcher().getUid().equals(user.getUid()));
            return DispatchCenterPerformanceDto.DispatcherMetric.builder()
                    .dispatcherUid(user.getUid())
                    .dispatcherName(user.getName())
                    .role(user.getRole().name())
                    .activeIncidents(active != 0 ? active : 0)
                    .resolvedToday(resolvedToday)
                    .onDuty(onDuty)
                    .build();
        }).collect(Collectors.toList());

        long totalHandled = allStaff.stream()
                .mapToLong(u -> incidentRepository.findAll().stream()
                        .filter(i -> u.getUid().equals(i.getAssignedDispatcher() != null
                                ? i.getAssignedDispatcher().getUid() : null))
                        .count())
                .sum();
        long resolved = allStaff.stream()
                .mapToLong(u -> incidentRepository.findAll().stream()
                        .filter(i -> u.getUid().equals(i.getAssignedDispatcher() != null
                                ? i.getAssignedDispatcher().getUid() : null)
                                && (i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED))
                        .count())
                .sum();
        long slaBreached = incidentRepository.findAll().stream()
                .filter(i -> i.getSlaBreachedAt() != null
                        && i.getAssignedDispatcher() != null
                        && allStaff.stream().anyMatch(u -> u.getUid().equals(i.getAssignedDispatcher().getUid())))
                .count();

        DispatchCenterPerformanceDto dto = DispatchCenterPerformanceDto.builder()
                .centerUid(center.getUid())
                .centerName(center.getName())
                .totalDispatchers(dispatchers.size())
                .totalSupervisors(supervisors.size())
                .onDutyNow(onDutyCount)
                .totalIncidentsHandled(totalHandled)
                .incidentsResolved(resolved)
                .incidentsPending(totalHandled - resolved)
                .slaBreachedIncidents(slaBreached)
                .dispatcherMetrics(metrics)
                .build();

        return Response.success(dto);
    }

    /** Delete (soft-delete) a Dispatch Center. ROOT only. */
    @Authenticated
    @RequiresPermission(Permission.DELETE_DISPATCH_CENTER)
    @DeleteMapping("/{uid}")
    public Response<EmergencyUnit> delete(@PathVariable String uid) {
        log.info("Deleting dispatch center: {}", uid);
        return unitService.deleteUnit(uid);
    }
}
