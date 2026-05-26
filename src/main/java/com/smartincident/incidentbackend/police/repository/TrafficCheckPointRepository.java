
package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TrafficCheckPointRepository extends JpaRepository<TrafficCheckpoint, Long> {
    Optional<TrafficCheckpoint> findByUid(String checkpointUid);

    @Query("select tc from TrafficCheckpoint tc where (:isActive is null or tc.isActive = :isActive) and " +
            "(lower(tc.name) like %:key% or lower(tc.location.address) like %:key% or lower(tc.parentStation.name) like %:key%)")
    Page<TrafficCheckpoint> getCheckpoints(Pageable pageable, Boolean isActive, String key);

    @Query("select tc from TrafficCheckpoint tc where (:isActive is null or tc.isActive = :isActive) and " +
            "(lower(tc.name) like %:key% or lower(tc.location.address) like %:key% or lower(tc.parentStation.name) like %:key%) " +
            "and tc.parentStation.uid = :stationUid")
    Page<TrafficCheckpoint> getTrafficCheckpointsByPoliceStation(Pageable pageable, Boolean isActive, String key, String stationUid);

    /** UIDs of officers currently serving as checkpoint supervisors at the given station. */
    @Query("select tc.supervisingOfficer.uid from TrafficCheckpoint tc " +
           "where tc.supervisingOfficer is not null and tc.isActive = true " +
           "and tc.parentStation.uid = :stationUid")
    java.util.List<String> findSupervisorOfficerUidsByStation(String stationUid);
}