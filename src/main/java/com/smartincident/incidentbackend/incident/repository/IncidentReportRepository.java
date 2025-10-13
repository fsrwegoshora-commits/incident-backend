package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    // Find by UID
    Optional<IncidentReport> findByUid(String uid);

    // Find by reporter (Citizen)
    @Query("SELECT i FROM IncidentReport i WHERE i.reportedBy.uid = :userUid " +
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

    // Find by police station
    @Query("SELECT i FROM IncidentReport i WHERE i.assignedStation.uid = :stationUid " +
            "AND (:isActive IS NULL OR i.isActive = :isActive) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :key, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :key, '%'))) " +
            "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findByStation(
            @Param("stationUid") String stationUid,
            @Param("status") IncidentStatus status,
            @Param("isActive") Boolean isActive,
            @Param("key") String key,
            Pageable pageable
    );

    // Find by assigned officer
    @Query("SELECT i FROM IncidentReport i WHERE i.assignedOfficer.uid = :officerUid " +
            "AND (:isActive IS NULL OR i.isActive = :isActive) " +
            "AND (:status IS NULL OR i.status = :status) " +
            "ORDER BY i.reportedAt DESC")
    Page<IncidentReport> findByOfficer(
            @Param("officerUid") String officerUid,
            @Param("status") IncidentStatus status,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    // Find nearby incidents (within radius)
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

    // Count by status for station
    @Query("SELECT COUNT(i) FROM IncidentReport i " +
            "WHERE i.assignedStation.uid = :stationUid " +
            "AND i.status = :status " +
            "AND i.isActive = true")
    Long countByStationAndStatus(
            @Param("stationUid") String stationUid,
            @Param("status") IncidentStatus status
    );

    // Recent incidents (last 24 hours)
    @Query("SELECT i FROM IncidentReport i " +
            "WHERE i.reportedAt >= :since " +
            "AND (:stationUid IS NULL OR i.assignedStation.uid = :stationUid) " +
            "AND i.isActive = true " +
            "ORDER BY i.reportedAt DESC")
    List<IncidentReport> findRecentIncidents(
            @Param("since") LocalDateTime since,
            @Param("stationUid") String stationUid
    );
}