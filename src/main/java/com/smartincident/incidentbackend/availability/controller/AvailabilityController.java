package com.smartincident.incidentbackend.availability.controller;

import com.smartincident.incidentbackend.availability.dto.ForceAvailabilityDto;
import com.smartincident.incidentbackend.availability.service.AvailabilityService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT','AGENCY_ADMIN','STATION_ADMIN','DISPATCHER','DISPATCHER_SUPERVISOR','DISPATCH_CENTER_ADMIN')")
    public ResponseEntity<Response<ForceAvailabilityDto>> getForceAvailability() {
        return ResponseEntity.ok(availabilityService.getForceAvailability());
    }
}
