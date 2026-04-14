package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.emergency.dto.VehicleLocationBroadcast;
import com.smartincident.incidentbackend.emergency.dto.VehicleLocationDto;
import com.smartincident.incidentbackend.emergency.entity.VehicleLocation;
import com.smartincident.incidentbackend.emergency.service.VehicleLocationService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles both:
 *  - WebSocket pings from crew devices (/app/vehicle/ping)
 *  - REST queries for location history
 */
@Controller
@RequiredArgsConstructor
public class VehicleLocationController {

    private final VehicleLocationService locationService;

    // ── WebSocket: crew device sends GPS ping ──────────────────────────────────

    /**
     * Device sends: STOMP SEND /app/vehicle/ping  body: VehicleLocationDto
     * Server saves, calculates ETA, broadcasts to:
     *   /topic/vehicle-location/{vehicleUid}
     *   /topic/incident-dispatch/{incidentUid}   (if responding to incident)
     */
    @MessageMapping("/vehicle/ping")
    public void handleLocationPing(@Payload VehicleLocationDto dto) {
        locationService.recordLocation(dto);
    }

    // ── REST: fallback HTTP ping (for devices that can't maintain WebSocket) ───

    @RestController
    @RequestMapping("/api/emergency/location")
    @RequiredArgsConstructor
    class LocationRestController {

        @Authenticated
        @AuthorizedRole({Role.FIRE_OFFICER, Role.MEDIC, Role.POLICE_OFFICER,
                Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
        @PostMapping("/ping")
        public Response<VehicleLocationBroadcast> ping(@RequestBody VehicleLocationDto dto) {
            return locationService.recordLocation(dto);
        }

        @Authenticated
        @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN,
                Role.FIRE_OFFICER, Role.MEDIC, Role.POLICE_OFFICER, Role.ROOT})
        @GetMapping("/{vehicleUid}/latest")
        public Response<VehicleLocation> getLatest(@PathVariable String vehicleUid) {
            return locationService.getLatestLocation(vehicleUid);
        }

        @Authenticated
        @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN,
                Role.FIRE_OFFICER, Role.MEDIC, Role.POLICE_OFFICER, Role.ROOT})
        @GetMapping("/{vehicleUid}/trail")
        public ResponseList<VehicleLocation> getTrail(
                @PathVariable String vehicleUid,
                @RequestParam(defaultValue = "60") int lastMinutes) {
            return new ResponseList<>(locationService.getTrail(vehicleUid, lastMinutes));
        }
    }
}
