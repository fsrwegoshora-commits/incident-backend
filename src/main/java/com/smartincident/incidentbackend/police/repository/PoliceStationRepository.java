package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.police.entity.PoliceStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface PoliceStationRepository extends JpaRepository<PoliceStation,Long> {
    Optional<PoliceStation> findByUid(String stationUid);

    Optional<PoliceStation> findByNameAndIsActiveTrue(String name);

    @Query("select p from PoliceStation p left join p.agency a " +
           "where lower(concat(p.name)) like %:key% " +
           "and (:isActive is null or p.isActive=:isActive) " +
           "and (:stationUid is null or p.uid=:stationUid) " +
           "and (:agencyUid is null or a.uid=:agencyUid)")
    Page<PoliceStation> getPoliceStations(Pageable pageable, Boolean isActive, String key, String stationUid, String agencyUid);

    /**
     * Used by AGENCY_ADMIN: returns stations belonging to their agency
     * AND stations not yet assigned to any agency (so they can claim them).
     */
    @Query("select p from PoliceStation p left join p.agency a " +
           "where lower(concat(p.name)) like %:key% " +
           "and (:isActive is null or p.isActive=:isActive) " +
           "and (a.uid = :agencyUid or a.uid is null)")
    Page<PoliceStation> getPoliceStationsForAgencyAdmin(
            Pageable pageable,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            @Param("agencyUid") String agencyUid);

    List<PoliceStation> findByIsActiveTrue();

    List<PoliceStation> findByAgencyUidAndIsActiveTrue(String agencyUid);
}
