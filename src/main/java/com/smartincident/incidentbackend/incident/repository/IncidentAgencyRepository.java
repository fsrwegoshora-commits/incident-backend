package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.enums.AgencyResponseStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.setting.entity.Agency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentAgencyRepository extends JpaRepository<IncidentAgency, Long> {

    Optional<IncidentAgency> findByUid(String uid);

    List<IncidentAgency> findByIncident(IncidentReport incident);

    List<IncidentAgency> findByIncidentAndAgency(IncidentReport incident, Agency agency);

    Page<IncidentAgency> findByAgencyAndResponseStatus(
            Agency agency,
            AgencyResponseStatus status,
            Pageable pageable
    );

    List<IncidentAgency> findByAgencyAndResponseStatusIn(
            Agency agency,
            List<AgencyResponseStatus> statuses
    );

    Long countByIncidentAndResponseStatus(IncidentReport incident, AgencyResponseStatus status);

    @Query("SELECT ia FROM IncidentAgency ia WHERE ia.incident.uid = :incidentUid AND ia.agency.uid = :agencyUid")
    Optional<IncidentAgency> findByIncidentUidAndAgencyUid(
            @Param("incidentUid") String incidentUid,
            @Param("agencyUid") String agencyUid);

    @Query("SELECT ia FROM IncidentAgency ia WHERE ia.incident.uid = :incidentUid")
    List<IncidentAgency> findByIncidentUid(@Param("incidentUid") String incidentUid);

    // Get all pending notifications for an agency
    Page<IncidentAgency> findByAgencyAndResponseStatus(
            Agency agency,
            AgencyResponseStatus status,
            PageRequest pageable
    );
}

