package com.smartincident.incidentbackend.fire.repository;

import com.smartincident.incidentbackend.fire.entity.FireVehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FireVehicleRepository extends JpaRepository<FireVehicle, Long> {

    Optional<FireVehicle> findByUid(String uid);

    boolean existsByPlateNumber(String plateNumber);

    @Query("""
        select v from FireVehicle v
        where (:key = '' or lower(concat(v.plateNumber, v.model)) like %:key%)
          and (:isActive is null or v.isActive = :isActive)
          and (:unitUid is null or v.emergencyUnit.uid = :unitUid)
        order by v.vehicleType, v.plateNumber
        """)
    Page<FireVehicle> findFireVehicles(Pageable pageable, Boolean isActive, String key, String unitUid);

    @Query("""
        select v from FireVehicle v
        where v.emergencyUnit.uid = :unitUid
          and v.isActive = true
          and v.status = 'AVAILABLE'
        order by v.vehicleType, v.plateNumber
        """)
    List<FireVehicle> findAvailableByUnit(String unitUid);
}
