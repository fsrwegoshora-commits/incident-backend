package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.police.entity.PoliceStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PoliceStationRepository extends JpaRepository<PoliceStation,Long> {
    Optional<PoliceStation> findByUid(String stationUid);

    Optional<PoliceStation> findByNameAndIsActiveTrue(String name);

    @Query("select p from PoliceStation p where lower(concat(p.name)) like %:key% and (:isActive is null or p.isActive=:isActive) and (:stationUid is null or p.uid=:stationUid)")
    Page<PoliceStation> getPoliceStations(Pageable pageable, Boolean isActive, String key,String stationUid);

    List<PoliceStation> findByIsActiveTrue();
}
