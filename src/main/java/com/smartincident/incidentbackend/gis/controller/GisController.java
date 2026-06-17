package com.smartincident.incidentbackend.gis.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.gis.dto.HeatmapDto;
import com.smartincident.incidentbackend.gis.dto.LiveResourceDto;
import com.smartincident.incidentbackend.gis.service.GisService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gis")
@RequiredArgsConstructor
@Slf4j
public class GisController {

    private final GisService gisService;

    /**
     * Live resource map — all active vehicles with GPS, active incidents,
     * and all emergency unit locations.
     */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT,
                     Role.POLICE_OFFICER, Role.FIRE_OFFICER, Role.MEDIC})
    @GetMapping("/resources")
    public Response<LiveResourceDto> getLiveResources() {
        log.info("Fetching live resource map data");
        return gisService.getLiveResources();
    }

    /**
     * GIS heatmap — incident density by geographic grid cells.
     *
     * @param type     optional incident type filter (e.g. THEFT, FIRE, ACCIDENT)
     * @param days     look-back window in days (default 30, max 365)
     * @param gridSize grid cell size in degrees (default 0.05 ≈ 5.5 km)
     */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.AGENCY_ADMIN, Role.ROOT, Role.STATION_ADMIN})
    @GetMapping("/heatmap")
    public Response<HeatmapDto> getHeatmap(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0.05") double gridSize) {
        if (days < 1 || days > 365) days = 30;
        if (gridSize < 0.01 || gridSize > 5.0) gridSize = 0.05;
        log.info("Heatmap request — type={}, days={}, gridSize={}", type, days, gridSize);
        return gisService.getHeatmap(type, days, gridSize);
    }
}
