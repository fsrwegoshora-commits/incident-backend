package com.smartincident.incidentbackend.dispatch.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.dispatch.dto.DispatchRecommendationResult;
import com.smartincident.incidentbackend.dispatch.service.DispatchRecommendationService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Smart dispatch recommendation endpoints.
 *
 * GET /api/dispatch/recommend/{incidentUid}
 *   → Nearest responders for an existing incident.
 *
 * GET /api/dispatch/recommend/coords?lat=&lng=&police=&fire=&medical=
 *   → Ad-hoc recommendation by coordinates (before incident is created).
 */
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchRecommendationController {

    private final DispatchRecommendationService service;

    @Authenticated
    @RequiresPermission(Permission.DISPATCH_INCIDENT)
    @GetMapping("/recommend/{incidentUid}")
    public Response<DispatchRecommendationResult> recommendForIncident(
            @PathVariable String incidentUid) {
        try {
            return Response.success(service.recommend(incidentUid));
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @Authenticated
    @RequiresPermission(Permission.DISPATCH_INCIDENT)
    @GetMapping("/recommend/coords")
    public Response<DispatchRecommendationResult> recommendByCoords(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "true")  boolean police,
            @RequestParam(defaultValue = "false")  boolean fire,
            @RequestParam(defaultValue = "false")  boolean medical) {
        return Response.success(service.recommendByCoords(lat, lng, police, fire, medical));
    }
}
