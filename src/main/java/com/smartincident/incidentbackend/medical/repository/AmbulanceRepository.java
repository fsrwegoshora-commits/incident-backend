package com.smartincident.incidentbackend.medical.repository;

import com.smartincident.incidentbackend.medical.entity.Ambulance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

    Optional<Ambulance> findByUid(String uid);

    boolean existsByPlateNumber(String plateNumber);

    @Query("""
        select a from Ambulance a
        where (:key = '' or lower(concat(a.plateNumber, a.model)) like %:key%)
          and (:isActive is null or a.isActive = :isActive)
          and (:unitUid is null or a.emergencyUnit.uid = :unitUid)
        order by a.vehicleType, a.plateNumber
        """)
    Page<Ambulance> findAmbulances(Pageable pageable, Boolean isActive, String key, String unitUid);

    @Query("""
        select a from Ambulance a
        where a.baseHospital.uid = :hospitalUid
          and a.isActive = true
        order by a.vehicleType, a.plateNumber
        """)
    List<Ambulance> findByHospitalUid(String hospitalUid);

    @Query("""
        select a from Ambulance a
        where a.emergencyUnit.uid = :unitUid
          and a.isActive = true
          and a.status = 'AVAILABLE'
        order by a.vehicleType, a.plateNumber
        """)
    List<Ambulance> findAvailableByUnit(String unitUid);
}
