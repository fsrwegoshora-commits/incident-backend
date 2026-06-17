package com.smartincident.incidentbackend.emergency.repository;

import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleShiftRepository extends JpaRepository<VehicleShift, Long> {

    Optional<VehicleShift> findByUid(String uid);

    @Query("""
        select s from VehicleShift s
        where s.vehicle.emergencyUnit.uid = :stationUid
          and (:isActive is null or s.isActive = :isActive)
        order by s.shiftDate desc, s.startTime
        """)
    Page<VehicleShift> findByStationUid(Pageable pageable, Boolean isActive, String stationUid);

    @Query("""
        select s from VehicleShift s
        where s.vehicle.uid = :vehicleUid
          and (:isActive is null or s.isActive = :isActive)
        order by s.shiftDate desc, s.startTime
        """)
    Page<VehicleShift> findByVehicleUid(Pageable pageable, Boolean isActive, String vehicleUid);

    @Query("""
        select s from VehicleShift s
        where s.vehicle.uid = :vehicleUid
          and s.shiftDate = :date
          and s.isActive = true
        """)
    List<VehicleShift> findByVehicleUidAndDate(String vehicleUid, LocalDate date);

    boolean existsByVehicleUidAndShiftDate(String vehicleUid, LocalDate shiftDate);

    @Query("""
        select s from VehicleShift s
        where s.shiftDate = :date
          and s.isActive = true
        """)
    List<VehicleShift> findActiveByDate(LocalDate date);
}
