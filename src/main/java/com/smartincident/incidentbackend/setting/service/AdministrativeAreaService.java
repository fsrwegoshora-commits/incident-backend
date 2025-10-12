package com.smartincident.incidentbackend.setting.service;

import com.smartincident.incidentbackend.enums.AdministrativeAreaLevel;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import com.smartincident.incidentbackend.setting.repository.AdministrativeAreaRepository;
import com.smartincident.incidentbackend.setting.repository.AreaTypeRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log
@RequiredArgsConstructor
public class AdministrativeAreaService {

    private final AdministrativeAreaRepository administrativeAreaRepository;

    private final AreaTypeRepository areaTypeRepository;

    public ResponsePage<AdministrativeArea> getAdministrativeAreas(PageableParam pageableParam, List<AdministrativeAreaLevel> areaLevels) {
        log.info("User with email " + LoggedUser.getName() + " is accessing Administrative Areas");
        try {
            Page<AdministrativeArea> administrativeAreas = administrativeAreaRepository.getAdministrativeAreas(pageableParam.getPageable(false), pageableParam.key(),areaLevels);
            return new ResponsePage<>(administrativeAreas);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponsePage<>(e.getLocalizedMessage());
        }
    }
    public ResponsePage<AreaType> getAreaType(PageableParam pageableParam, List<AdministrativeAreaLevel> areaLevels){
        log.info("User with email " + LoggedUser.getName() + " is accessing Area types");
        try{
            Page<AreaType> areas = areaTypeRepository.getAreaType(pageableParam.getPageable(false), pageableParam.key(), areaLevels);
            return new ResponsePage<>(areas);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ResponsePage<>(e.getLocalizedMessage());
        }
    }

}
