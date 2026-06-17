package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.enums.CorrelationType;
import com.smartincident.incidentbackend.incident.entity.IncidentCorrelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentCorrelationRepository extends JpaRepository<IncidentCorrelation, Long> {

    /** All correlations where this incident is the master record. */
    @Query("SELECT c FROM IncidentCorrelation c WHERE c.masterIncident.uid = :uid AND c.isActive = true")
    List<IncidentCorrelation> findByMasterUid(@Param("uid") String uid);

    /** All correlations where this incident is the linked (duplicate/related) record. */
    @Query("SELECT c FROM IncidentCorrelation c WHERE c.linkedIncident.uid = :uid AND c.isActive = true")
    List<IncidentCorrelation> findByLinkedUid(@Param("uid") String uid);

    /** Combined: all correlations involving this incident on either side. */
    @Query("SELECT c FROM IncidentCorrelation c " +
           "WHERE (c.masterIncident.uid = :uid OR c.linkedIncident.uid = :uid) " +
           "AND c.isActive = true ORDER BY c.detectedAt DESC")
    List<IncidentCorrelation> findAllByIncidentUid(@Param("uid") String uid);

    /** Only DUPLICATE correlations (suppressed reports) for a master incident. */
    @Query("SELECT c FROM IncidentCorrelation c " +
           "WHERE c.masterIncident.uid = :uid " +
           "AND c.correlationType = :type " +
           "AND c.isActive = true")
    List<IncidentCorrelation> findByMasterUidAndType(
            @Param("uid") String uid,
            @Param("type") CorrelationType type);

    /** Count how many duplicate reports have been merged into this master. */
    @Query("SELECT COUNT(c) FROM IncidentCorrelation c " +
           "WHERE c.masterIncident.uid = :uid " +
           "AND c.correlationType = 'DUPLICATE' " +
           "AND c.isActive = true")
    long countDuplicatesByMaster(@Param("uid") String uid);
}
