package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.incident.entity.AfterActionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AfterActionReportRepository extends JpaRepository<AfterActionReport, Long> {

    Optional<AfterActionReport> findByUid(String uid);

    Optional<AfterActionReport> findByIncidentUid(String incidentUid);

    @Query("select a from AfterActionReport a where " +
           "(:approved is null or a.isApproved = :approved) " +
           "and (:authorUid is null or a.authoredBy.uid = :authorUid) " +
           "order by a.createdAt desc")
    Page<AfterActionReport> findFiltered(Boolean approved, String authorUid, Pageable pageable);
}
