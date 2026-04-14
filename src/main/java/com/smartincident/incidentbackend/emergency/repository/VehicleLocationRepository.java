package com.smartincident.incidentbackend.emergency.repository;

import com.smartincident.incidentbackend.emergency.entity.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {

    @Query("""
        select l from VehicleLocation l
        where l.vehicle.uid = :vehicleUid
        order by l.recordedAt desc
        limit 1
        """)
    Optional<VehicleLocation> findLatestByVehicleUid(String vehicleUid);

    @Query("""
        select l from VehicleLocation l
        where l.vehicle.uid = :vehicleUid
          and l.recordedAt >= :since
        order by l.recordedAt asc
        """)
    List<VehicleLocation> findTrailSince(String vehicleUid, LocalDateTime since);

    // Delete old GPS pings to prevent table bloat (keep last 24h per vehicle)
    void deleteByVehicleUidAndRecordedAtBefore(String vehicleUid, LocalDateTime cutoff);
}
