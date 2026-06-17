package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.incident.entity.IncidentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IncidentStatusHistoryRepository extends JpaRepository<IncidentStatusHistory, Long> {

    @Query("select h from IncidentStatusHistory h where h.incident.uid = :incidentUid order by h.changedAt asc")
    List<IncidentStatusHistory> findByIncidentUid(String incidentUid);
}
