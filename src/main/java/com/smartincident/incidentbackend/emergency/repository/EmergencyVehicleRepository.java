package com.smartincident.incidentbackend.emergency.repository;

import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmergencyVehicleRepository extends JpaRepository<EmergencyVehicle, Long> {

    Optional<EmergencyVehicle> findByUid(String uid);

    @Query("""
        select v from EmergencyVehicle v
        where (:key = '' or lower(concat(v.plateNumber, v.model)) like %:key%)
          and (:isActive is null or v.isActive = :isActive)
          and (:unitUid is null or v.emergencyUnit.uid = :unitUid)
        order by v.vehicleType, v.plateNumber
        """)
    Page<EmergencyVehicle> findVehicles(Pageable pageable, Boolean isActive, String key, String unitUid);

    @Query("""
        select v from EmergencyVehicle v
        where v.emergencyUnit.uid = :unitUid
          and v.isActive = true
          and v.status = :status
        order by v.vehicleType, v.plateNumber
        """)
    List<EmergencyVehicle> findByUnitUidAndStatus(String unitUid, VehicleStatus status);

    @Query("""
        select v from EmergencyVehicle v
        where v.emergencyUnit.uid = :unitUid
          and v.isActive = true
          and v.status = 'AVAILABLE'
          and (:vehicleType is null or v.vehicleType = :vehicleType)
        order by v.vehicleType, v.plateNumber
        """)
    List<EmergencyVehicle> findAvailableAtStation(String unitUid, VehicleType vehicleType);

    boolean existsByPlateNumber(String plateNumber);
}
