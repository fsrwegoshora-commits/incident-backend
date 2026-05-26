package com.smartincident.incidentbackend.emergency.repository;

import com.smartincident.incidentbackend.emergency.entity.IncidentDispatch;
import com.smartincident.incidentbackend.enums.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IncidentDispatchRepository extends JpaRepository<IncidentDispatch, Long> {

    Optional<IncidentDispatch> findByUid(String uid);

    List<IncidentDispatch> findByIncidentUidOrderByDispatchedAtDesc(String incidentUid);

    List<IncidentDispatch> findByVehicleUidOrderByDispatchedAtDesc(String vehicleUid);

    @Query("""
        select d from IncidentDispatch d
        where d.vehicle.emergencyUnit.uid = :stationUid
          and d.status not in ('CLEARED', 'CANCELLED')
        order by d.dispatchedAt desc
        """)
    List<IncidentDispatch> findActiveDispatchesByStation(String stationUid);

    boolean existsByVehicleUidAndStatusNotIn(String vehicleUid, List<DispatchStatus> statuses);
}
