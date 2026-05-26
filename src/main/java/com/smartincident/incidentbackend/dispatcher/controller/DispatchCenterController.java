package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.dispatcher.dto.DispatcherShiftDto;
import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.service.DispatcherShiftService;
import com.smartincident.incidentbackend.emergency.dto.EmergencyUnitDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.service.EmergencyUnitService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatch-centers")
@RequiredArgsConstructor
@Slf4j
public class DispatchCenterController {

    private final EmergencyUnitService unitService;
    private final UserRepository userRepository;
    private final DispatcherShiftService shiftService;

    /** Create or update a Dispatch Center. ROOT / AGENCY_ADMIN only. */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN})
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
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR})
    @GetMapping("/{uid}/dispatchers")
    public ResponseList<User> getDispatchers(@PathVariable String uid) {
        log.info("Fetching dispatchers for center: {}", uid);
        List<User> dispatchers = userRepository.findByRoleInAndEmergencyUnitUidAndIsActiveTrue(
                List.of(Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR), uid);
        return new ResponseList<>(dispatchers);
    }

    /** List all shifts for dispatchers in a Dispatch Center. */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR})
    @GetMapping("/{uid}/shifts")
    public ResponseList<DispatcherShift> getShifts(@PathVariable String uid) {
        log.info("Fetching dispatcher shifts for center: {}", uid);
        return shiftService.getShiftsByDispatchCenter(uid);
    }

    /** Create a dispatcher shift — available to DISPATCH_CENTER_ADMIN and above. */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR})
    @PostMapping("/{uid}/shifts")
    public Response<DispatcherShift> createShift(@PathVariable String uid,
                                                  @RequestBody DispatcherShiftDto dto) {
        log.info("Creating dispatcher shift for center: {}", uid);
        return shiftService.saveShift(dto);
    }

    /** Delete (soft-delete) a Dispatch Center. ROOT only. */
    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<EmergencyUnit> delete(@PathVariable String uid) {
        log.info("Deleting dispatch center: {}", uid);
        return unitService.deleteUnit(uid);
    }
}
