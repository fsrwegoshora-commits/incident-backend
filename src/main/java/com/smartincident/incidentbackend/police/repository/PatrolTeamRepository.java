package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.enums.PatrolTeamStatus;
import com.smartincident.incidentbackend.police.entity.PatrolTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatrolTeamRepository extends JpaRepository<PatrolTeam, Long> {

    Optional<PatrolTeam> findByUid(String uid);

    @Query("""
        select t from PatrolTeam t
        where t.station.uid = :stationUid
          and (:isActive is null or t.isActive = :isActive)
          and (lower(t.name) like %:key% or lower(coalesce(t.coverageArea,'')) like %:key%)
        order by t.createdAt desc
        """)
    Page<PatrolTeam> findByStation(Pageable pageable, Boolean isActive, String key,
                                   @Param("stationUid") String stationUid);

    @Query("""
        select t from PatrolTeam t
        left join t.station s left join s.agency a
        where (a.uid = :agencyUid or a.uid is null)
          and (:isActive is null or t.isActive = :isActive)
          and (lower(t.name) like %:key% or lower(coalesce(t.coverageArea,'')) like %:key%)
        order by t.createdAt desc
        """)
    Page<PatrolTeam> findByAgency(Pageable pageable, Boolean isActive, String key,
                                  @Param("agencyUid") String agencyUid);

    List<PatrolTeam> findByStationUidAndStatusAndIsActiveTrue(String stationUid, PatrolTeamStatus status);

    long countByStationUidAndStatusAndIsActiveTrue(String stationUid, PatrolTeamStatus status);
}
