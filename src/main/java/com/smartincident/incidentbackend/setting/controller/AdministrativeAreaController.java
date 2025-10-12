package com.smartincident.incidentbackend.setting.controller;

import com.smartincident.incidentbackend.enums.AdministrativeAreaLevel;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.service.AdministrativeAreaService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.ResponsePage;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;

@Controller
@GraphQLApi
@RequiredArgsConstructor
public class AdministrativeAreaController {

    private final AdministrativeAreaRepository administrativeAreaRepository;

    private final AdministrativeAreaService administrativeAreaService;

    @GraphQLQuery(name = "getAdministrativeAreas")
    public ResponsePage<AdministrativeArea> findByAdministrativeAreaName(
            @GraphQLArgument(name = "pageableParam") PageableParam pageableParam,
            List<AdministrativeAreaLevel> areaLevels) {

        if (pageableParam == null)
            pageableParam = new PageableParam();

        // Allow searching all levels if areaLevels is null or empty
        if (areaLevels == null || areaLevels.isEmpty()) {
            areaLevels = Arrays.asList(AdministrativeAreaLevel.values());
        }

        return administrativeAreaService.getAdministrativeAreas(
                pageableParam != null ? pageableParam : new PageableParam(),
                areaLevels
        );
    }

    @GraphQLQuery(name = "getAreaTypes")
    public ResponsePage<AreaType> findByAreaType(@GraphQLArgument(name="pageableParam") PageableParam pageableParam, List<AdministrativeAreaLevel> areaLevels){
        if(pageableParam == null){
            pageableParam = new PageableParam();
        }
        return administrativeAreaService.getAreaType(pageableParam!=null?pageableParam:new PageableParam(),areaLevels);
    }
}
