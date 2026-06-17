package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.enums.IncidentNature;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    Optional<IncidentReport> findByUid(String uid);

    /** Relationships rendered on every list row in the portal — fetched eagerly to avoid N+1. */
    String LIST_FETCH = "LEFT JOIN FETCH i.reportedBy " +
            "LEFT JOIN FETCH i.assignedPoliceStation " +
            "LEFT JOIN FETCH i.assignedUnit " +
            "LEFT JOIN FETCH i.assignedOfficer " +
            "LEFT JOIN FETCH i.assignedDispatcher ";

    // ── Citizen: incidents reported by a user ──────────────────────────────
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.reportedBy.uid = :userUid " +
           "AND (:isActive IS NULL OR i.isActive = :isActive) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findByReporter(
            @Param("userUid") String userUid,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            Pageable pageable
    );

    // ── Fire / Medical unit: incidents assigned to an EmergencyUnit ────────
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.assignedUnit.uid = :unitUid " +
           "AND (:isActive IS NULL OR i.isActive = :isActive) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findByEmergencyUnit(
            @Param("unitUid") String unitUid,
            @Param("status") IncidentStatus status,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            Pageable pageable
    );

    // ── Police: incidents assigned to a PoliceStation ──────────────────────
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.assignedPoliceStation.uid = :stationUid " +
           "AND (:isActive IS NULL OR i.isActive = :isActive) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findByPoliceStation(
            @Param("stationUid") String stationUid,
            @Param("status") IncidentStatus status,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            Pageable pageable
    );

    // ── Assigned officer ───────────────────────────────────────────────────
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.assignedOfficer.uid = :officerUid " +
           "AND (:isActive IS NULL OR i.isActive = :isActive) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findByOfficer(
            @Param("officerUid") String officerUid,
            @Param("status") IncidentStatus status,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            Pageable pageable
    );

    // ── Nearby incidents (Haversine, native query) ─────────────────────────
    @Query(value = "SELECT * FROM incidents i " +
                   "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(i.latitude)) * " +
                   "cos(radians(i.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
                   "sin(radians(i.latitude)))) <= :radiusKm " +
                   "AND (:status IS NULL OR i.status = :status) " +
                   "AND i.is_active = true " +
                   "ORDER BY i.reported_at DESC",
           nativeQuery = true)
    List<IncidentReport> findNearbyIncidents(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("status") String status
    );

    // ── Correlation engine: candidate search ──────────────────────────────

    /**
     * Returns active, non-duplicate incidents reported within {@code radiusKm} of
     * the given point and within the specified time window.
     * Used exclusively by {@code IncidentCorrelationService.detect()}.
     */
    @Query(value =
        "SELECT * FROM incidents i " +
        "WHERE (6371 * acos(" +
        "  cos(radians(:lat)) * cos(radians(i.latitude)) * " +
        "  cos(radians(i.longitude) - radians(:lon)) + " +
        "  sin(radians(:lat)) * sin(radians(i.latitude))" +
        ")) <= :radiusKm " +
        "AND i.reported_at  >= :since " +
        "AND i.is_active     = true " +
        "AND i.is_duplicate  = false " +
        "AND i.status NOT IN ('RESOLVED','CLOSED','REJECTED') " +
        "ORDER BY i.reported_at DESC",
        nativeQuery = true)
    List<IncidentReport> findCandidatesForCorrelation(
            @Param("lat")      Double lat,
            @Param("lon")      Double lon,
            @Param("radiusKm") double radiusKm,
            @Param("since")    LocalDateTime since);

    // ── Statistics ────────────────────────────────────────────────────────

    @Query("SELECT COUNT(i) FROM IncidentReport i " +
           "WHERE i.assignedUnit.uid = :unitUid AND i.status = :status AND i.isActive = true")
    Long countByUnitAndStatus(@Param("unitUid") String unitUid, @Param("status") IncidentStatus status);

    @Query("SELECT COUNT(i) FROM IncidentReport i " +
           "WHERE i.assignedPoliceStation.uid = :stationUid AND i.status = :status AND i.isActive = true")
    Long countByPoliceStationAndStatus(
            @Param("stationUid") String stationUid, @Param("status") IncidentStatus status);

    /** @deprecated Use countByUnitAndStatus or countByPoliceStationAndStatus */
    @Deprecated
    @Query("SELECT COUNT(i) FROM IncidentReport i " +
           "WHERE (i.assignedUnit.uid = :stationUid OR i.assignedPoliceStation.uid = :stationUid) " +
           "AND i.status = :status AND i.isActive = true")
    Long countByStationAndStatus(
            @Param("stationUid") String stationUid, @Param("status") IncidentStatus status);

    @Query("SELECT i FROM IncidentReport i " +
           "WHERE i.reportedAt >= :since " +
           "AND (:stationUid IS NULL " +
           "     OR i.assignedUnit.uid = :stationUid " +
           "     OR i.assignedPoliceStation.uid = :stationUid) " +
           "AND i.isActive = true " +
           "ORDER BY i.reportedAt DESC")
    List<IncidentReport> findRecentIncidents(
            @Param("since") LocalDateTime since,
            @Param("stationUid") String stationUid
    );

    // ── Dispatch Queue: incidents assigned to a specific dispatcher ───────────

    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.assignedDispatcher.uid = :dispatcherUid " +
           "AND i.isActive = true " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "  OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findDispatchQueue(
            @Param("dispatcherUid") String dispatcherUid,
            @Param("status") IncidentStatus status,
            @Param("key") String key,
            Pageable pageable);

    /** All incidents assigned to any dispatcher in a specific dispatch center. */
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.assignedDispatcher.emergencyUnit.uid = :centerUid " +
           "AND i.isActive = true " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "  OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findDispatchQueueByCenter(
            @Param("centerUid") String centerUid,
            @Param("status") IncidentStatus status,
            @Param("key") String key,
            Pageable pageable);

    /** All active incidents regardless of assignment — ROOT/AGENCY_ADMIN oversight view. */
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.isActive = true " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findAllActiveIncidents(@Param("status") IncidentStatus status, Pageable pageable);

    // ── Pending / new incidents (lifecycle-based) ──────────────────────────

    /** All pre-dispatch incidents across every queue — for ROOT/AGENCY_ADMIN oversight. */
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.isActive = true " +
           "AND i.status IN (" +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.REPORTED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.PENDING, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.UNDER_REVIEW, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.CLASSIFIED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.WAITING_FOR_DISPATCH) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findAllPendingIncidents(Pageable pageable);

    /** Pending incidents at a specific police station — used by the dispatcher assignment panel. */
    @Query("SELECT i FROM IncidentReport i " + LIST_FETCH +
           "WHERE i.assignedPoliceStation.uid = :stationUid " +
           "AND i.isActive = true " +
           "AND i.status IN (" +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.REPORTED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.PENDING, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.UNDER_REVIEW, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.CLASSIFIED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.WAITING_FOR_DISPATCH) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findPendingByPoliceStation(@Param("stationUid") String stationUid, Pageable pageable);

    // ── SLA queries — on-demand (paginated) ─────────────────────────────────

    @Query("SELECT i FROM IncidentReport i WHERE i.isActive = true AND i.slaBreachedAt IS NOT NULL " +
           "ORDER BY i.slaBreachedAt DESC")
    Page<IncidentReport> findSlaBreached(Pageable pageable);

    @Query("SELECT i FROM IncidentReport i WHERE i.isActive = true AND i.slaDeadline IS NOT NULL " +
           "AND i.slaDeadline > :now AND i.slaDeadline < :threshold AND i.slaBreachedAt IS NULL " +
           "ORDER BY i.slaDeadline ASC")
    Page<IncidentReport> findSlaApproaching(
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable);

    // ── SLA monitoring — scheduled-job candidates (unpaginated, batch) ─────

    @Query("SELECT i FROM IncidentReport i WHERE i.isActive = true " +
           "AND i.status IN :statuses " +
           "AND i.slaDeadline IS NOT NULL AND i.slaDeadline < :now AND i.slaBreachedAt IS NULL")
    List<IncidentReport> findSlaBreachCandidates(
            @Param("statuses") Collection<IncidentStatus> statuses,
            @Param("now") LocalDateTime now);

    @Query("SELECT i FROM IncidentReport i WHERE i.isActive = true " +
           "AND i.status IN :statuses " +
           "AND i.slaDeadline IS NOT NULL AND i.slaDeadline > :now AND i.slaDeadline < :threshold " +
           "AND i.slaBreachedAt IS NULL")
    List<IncidentReport> findSlaApproachingCandidates(
            @Param("statuses") Collection<IncidentStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold);

    /** All active operational incidents (DISPATCHED through IN_PROGRESS) for real-time monitoring. */
    @Query("SELECT i FROM IncidentReport i " +
           "WHERE i.isActive = true " +
           "AND i.status IN (" +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.DISPATCHED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.ACKNOWLEDGED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.EN_ROUTE, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.AT_SCENE, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.IN_PROGRESS) " +
           "ORDER BY i.reportedAt DESC")
    List<IncidentReport> findOperationalIncidents();

    /** Count unresolved incidents assigned to a dispatcher — used for least-active load balancing. */
    @Query("SELECT COUNT(i) FROM IncidentReport i " +
           "WHERE i.assignedDispatcher.uid = :uid " +
           "AND i.isActive = true " +
           "AND i.status NOT IN (" +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.RESOLVED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.CLOSED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.REJECTED)")
    Long countActiveIncidentsByDispatcher(@Param("uid") String uid);

    @Query("SELECT COUNT(i) FROM IncidentReport i " +
           "WHERE i.assignedDispatcher.uid = :dispatcherUid " +
           "AND i.status = :status AND i.isActive = true")
    Long countByDispatcherAndStatus(
            @Param("dispatcherUid") String dispatcherUid,
            @Param("status") IncidentStatus status);

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(i) FROM IncidentReport i WHERE i.isActive = true " +
           "AND (:agencyUid IS NULL OR i.leadAgency.uid = :agencyUid)")
    Long countTotal(@Param("agencyUid") String agencyUid);

    @Query("SELECT COUNT(i) FROM IncidentReport i WHERE i.isActive = true " +
           "AND i.status = :status " +
           "AND (:agencyUid IS NULL OR i.leadAgency.uid = :agencyUid)")
    Long countByStatus(@Param("status") IncidentStatus status, @Param("agencyUid") String agencyUid);

    @Query("SELECT i.type, COUNT(i) FROM IncidentReport i WHERE i.isActive = true " +
           "AND (:agencyUid IS NULL OR i.leadAgency.uid = :agencyUid) " +
           "GROUP BY i.type ORDER BY COUNT(i) DESC")
    List<Object[]> countGroupedByType(@Param("agencyUid") String agencyUid);

    @Query("SELECT CAST(i.reportedAt AS date), COUNT(i) FROM IncidentReport i " +
           "WHERE i.isActive = true " +
           "AND i.reportedAt >= :since " +
           "AND (:agencyUid IS NULL OR i.leadAgency.uid = :agencyUid) " +
           "GROUP BY CAST(i.reportedAt AS date) ORDER BY CAST(i.reportedAt AS date)")
    List<Object[]> countGroupedByDay(@Param("since") LocalDateTime since, @Param("agencyUid") String agencyUid);

    // ── Performance metrics ───────────────────────────────────────────────────

    @Query("SELECT i FROM IncidentReport i WHERE i.isActive = true AND i.dispatchedAt IS NOT NULL " +
           "AND (:agencyUid IS NULL OR i.leadAgency.uid = :agencyUid)")
    List<IncidentReport> findDispatchedIncidents(@Param("agencyUid") String agencyUid);

    /** Returns [dispatcher_uid, dispatcher_name, total_count, active_count] per dispatcher */
    @Query("SELECT i.assignedDispatcher.uid, i.assignedDispatcher.name, COUNT(i), " +
           "SUM(CASE WHEN i.status NOT IN (" +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.RESOLVED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.CLOSED, " +
           "  com.smartincident.incidentbackend.enums.IncidentStatus.REJECTED) THEN 1L ELSE 0L END) " +
           "FROM IncidentReport i " +
           "WHERE i.isActive = true AND i.assignedDispatcher IS NOT NULL " +
           "GROUP BY i.assignedDispatcher.uid, i.assignedDispatcher.name " +
           "ORDER BY COUNT(i) DESC")
    List<Object[]> findDispatcherWorkload();

    @Query("SELECT COUNT(i) FROM IncidentReport i WHERE i.isActive = true AND i.escalatedAt IS NOT NULL " +
           "AND (:agencyUid IS NULL OR i.leadAgency.uid = :agencyUid)")
    Long countEscalated(@Param("agencyUid") String agencyUid);

    // ── Investigation queue — NON_EMERGENCY incidents at a station ─────────
    @Query("SELECT i FROM IncidentReport i " +
           "WHERE i.assignedPoliceStation.uid = :stationUid " +
           "AND i.nature = :nature " +
           "AND (:isActive IS NULL OR i.isActive = :isActive) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findInvestigationQueue(
            @Param("stationUid") String stationUid,
            @Param("nature") IncidentNature nature,
            @Param("status") IncidentStatus status,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            Pageable pageable
    );
}
