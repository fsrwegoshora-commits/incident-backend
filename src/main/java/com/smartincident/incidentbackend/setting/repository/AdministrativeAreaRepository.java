package com.smartincident.incidentbackend.setting.repository;

import com.smartincident.incidentbackend.enums.AdministrativeAreaLevel;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdministrativeAreaRepository extends JpaRepository<AdministrativeArea,Long> {
    @Query("SELECT e FROM AdministrativeArea e " +
            "LEFT JOIN FETCH e.areaType at " +
            "LEFT JOIN FETCH at.areaLevel al " +
            "WHERE lower(CONCAT(e.name, e.label, e.uid, " +
            "al.level, al.name, al.nameSw)) LIKE %:key% " +
            "AND (:areaLevels IS NULL OR al.level IN :areaLevels) " +
            "ORDER BY al.level, e.name")
    Page<AdministrativeArea> getAdministrativeAreas(
            Pageable pageable,
            @Param("key") String key,
            @Param("areaLevels") List<AdministrativeAreaLevel> areaLevels
    );

    Optional<AdministrativeArea> findByName(String name);

    Optional<AdministrativeArea> findByUid(String administrativeAreaUid);
}
