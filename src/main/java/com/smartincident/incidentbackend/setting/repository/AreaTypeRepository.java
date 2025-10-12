package com.smartincident.incidentbackend.setting.repository;

import com.smartincident.incidentbackend.enums.AdministrativeAreaLevel;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AreaTypeRepository extends JpaRepository<AreaType,Long> {

    @Query("select a " +
            "from AreaType a " +
            "where lower(concat(a.uid,a.name)) like %:key% and (:areaLevels is null or a.areaLevel.level in :areaLevels)")
    Page<AreaType> getAreaType(Pageable pageable, String key, List<AdministrativeAreaLevel> areaLevels);

    Optional<AreaType> findByName(String name);
}
