package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.police.entity.PoliceVehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PoliceVehicleRepository extends JpaRepository<PoliceVehicle, Long> {

    Optional<PoliceVehicle> findByUid(String uid);

    boolean existsByPlateNumber(String plateNumber);

    @Query("SELECT v FROM PoliceVehicle v " +
           "WHERE v.policeStation.uid = :stationUid " +
           "AND (:isActive IS NULL OR v.isActive = :isActive) " +
           "AND (LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "  OR LOWER(v.model) LIKE LOWER(CONCAT('%', :key, '%')))")
    Page<PoliceVehicle> findByStation(Pageable pageable, Boolean isActive, String key,
                                      @Param("stationUid") String stationUid);

    /** AGENCY_ADMIN scope: stations belonging to their agency OR unassigned. */
    @Query("SELECT v FROM PoliceVehicle v " +
           "LEFT JOIN v.policeStation s LEFT JOIN s.agency a " +
           "WHERE (:isActive IS NULL OR v.isActive = :isActive) " +
           "AND (LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "  OR LOWER(v.model) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "AND (a.uid = :agencyUid OR a.uid IS NULL)")
    Page<PoliceVehicle> findByAgency(Pageable pageable, Boolean isActive, String key,
                                     @Param("agencyUid") String agencyUid);

    @Query("SELECT COUNT(v) FROM PoliceVehicle v " +
           "WHERE v.policeStation.uid = :stationUid AND v.status = :status AND v.isActive = TRUE")
    Long countByStationAndStatus(@Param("stationUid") String stationUid,
                                 @Param("status") VehicleStatus status);
}
