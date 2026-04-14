package com.smartincident.incidentbackend.setting.controller;

import com.smartincident.incidentbackend.enums.AdministrativeAreaLevel;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.service.AdministrativeAreaService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AdministrativeAreaController {

    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final AdministrativeAreaService administrativeAreaService;

    @GetMapping
    public ResponsePage<AdministrativeArea> getAdministrativeAreas(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) List<AdministrativeAreaLevel> areaLevels) {

        if (pageableParam == null)
            pageableParam = new PageableParam();

        if (areaLevels == null || areaLevels.isEmpty()) {
            areaLevels = Arrays.asList(AdministrativeAreaLevel.values());
        }

        return administrativeAreaService.getAdministrativeAreas(pageableParam, areaLevels);
    }

    @GetMapping("/types")
    public ResponsePage<AreaType> getAreaTypes(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) List<AdministrativeAreaLevel> areaLevels) {

        if (pageableParam == null) {
            pageableParam = new PageableParam();
        }
        return administrativeAreaService.getAreaType(pageableParam, areaLevels);
    }
}
