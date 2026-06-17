package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.emergency.dto.VehicleLocationBroadcast;
import com.smartincident.incidentbackend.emergency.dto.VehicleLocationDto;
import com.smartincident.incidentbackend.emergency.entity.VehicleLocation;
import com.smartincident.incidentbackend.emergency.service.VehicleLocationService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket pings from crew devices (/app/vehicle/ping) and REST fallback for location queries.
 */
@Controller
@RequiredArgsConstructor
public class VehicleLocationController {

    private final VehicleLocationService locationService;

    @MessageMapping("/vehicle/ping")
    public void handleLocationPing(@Payload VehicleLocationDto dto) {
        locationService.recordLocation(dto);
    }

    @RestController
    @RequestMapping("/api/emergency/location")
    @RequiredArgsConstructor
    class LocationRestController {

        @Authenticated
        @RequiresPermission(Permission.VIEW_VEHICLES)
        @PostMapping("/ping")
        public Response<VehicleLocationBroadcast> ping(@RequestBody VehicleLocationDto dto) {
            return locationService.recordLocation(dto);
        }

        @Authenticated
        @RequiresPermission(Permission.VIEW_VEHICLES)
        @GetMapping("/{vehicleUid}/latest")
        public Response<VehicleLocation> getLatest(@PathVariable String vehicleUid) {
            return locationService.getLatestLocation(vehicleUid);
        }

        @Authenticated
        @RequiresPermission(Permission.VIEW_VEHICLES)
        @GetMapping("/{vehicleUid}/trail")
        public ResponseList<VehicleLocation> getTrail(
                @PathVariable String vehicleUid,
                @RequestParam(defaultValue = "60") int lastMinutes) {
            return new ResponseList<>(locationService.getTrail(vehicleUid, lastMinutes));
        }

        /** Returns all vehicles that have a known GPS position (for the live map). */
        @Authenticated
        @RequiresPermission(Permission.VIEW_VEHICLES)
        @GetMapping("/all-active")
        public ResponseList<?> getAllActiveLocations() {
            return locationService.getAllActiveVehiclePositions();
        }
    }
}
