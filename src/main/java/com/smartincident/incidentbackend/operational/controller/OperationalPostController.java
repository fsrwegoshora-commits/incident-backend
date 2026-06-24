package com.smartincident.incidentbackend.operational.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.PostType;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.operational.dto.OperationalPostDto;
import com.smartincident.incidentbackend.operational.service.OperationalPostService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operational-posts")
@RequiredArgsConstructor
public class OperationalPostController {

    private final OperationalPostService service;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<?> create(@RequestBody OperationalPostDto dto) {
        return service.save(dto);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<?> get(@PathVariable String uid) {
        return service.get(uid);
    }

    @Authenticated
    @GetMapping
    public ResponseList<?> getAll() {
        return service.getAll();
    }

    @Authenticated
    @GetMapping("/by-agency/{agencyUid}")
    public ResponseList<?> getByAgency(@PathVariable String agencyUid) {
        return service.getByAgency(agencyUid);
    }

    @Authenticated
    @GetMapping("/by-station/{stationUid}")
    public ResponseList<?> getByStation(@PathVariable String stationUid) {
        return service.getByParentStation(stationUid);
    }

    @Authenticated
    @GetMapping("/by-unit/{unitUid}")
    public ResponseList<?> getByUnit(@PathVariable String unitUid) {
        return service.getByParentUnit(unitUid);
    }

    @Authenticated
    @GetMapping("/by-hospital/{hospitalUid}")
    public ResponseList<?> getByHospital(@PathVariable String hospitalUid) {
        return service.getByParentHospital(hospitalUid);
    }

    @Authenticated
    @GetMapping("/nearby")
    public ResponseList<?> getNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "20") double radiusKm,
            @RequestParam PostType postType) {
        return service.getNearby(latitude, longitude, radiusKm, postType);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<?> delete(@PathVariable String uid) {
        return service.delete(uid);
    }
}
