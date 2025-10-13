package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.police.entity.OfficerShift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface OfficerShiftRepository extends JpaRepository<OfficerShift,Long> {
    Optional<OfficerShift> findByUid(String uid);

    boolean existsByOfficerUidAndShiftDate(String newOfficerUid, LocalDate shiftDate);

    @Query("""
    select o from OfficerShift o
    where lower(concat(o.officer.userAccount.name, o.officer.userAccount.phoneNumber)) like %:key%
      and (:isActive is null or o.isActive = :isActive)
    """)
    Page<OfficerShift> getPoliceOfficerShifts(Pageable pageable, Boolean isActive, String key);

    @Query("select o from OfficerShift o where lower(concat(o.officer.userAccount.name, o.officer.userAccount.phoneNumber)) like %:key% and (:isActive is null or o.isActive=:isActive) and o.officer.station.uid = :stationUid")
    Page<OfficerShift> getShiftsByStation(Pageable pageable, Boolean isActive, String key, String stationUid);

    @Query("select o from OfficerShift o where lower(concat(o.officer.userAccount.name, o.officer.userAccount.phoneNumber)) like %:key% and (:isActive is null or o.isActive=:isActive) and o.officer.uid = :policeOfficerUid")
    Page<OfficerShift> getShiftsByPoliceOfficer(Pageable pageable, Boolean isActive, String key, String policeOfficerUid);

    List<OfficerShift> findByOfficerUidAndShiftDate(String newOfficerUid, LocalDate shiftDate);

    Optional<OfficerShift> findByOfficerUidAndShiftDateAndStartTimeBeforeAndEndTimeAfter(String uid, LocalDate today, LocalTime now, LocalTime now1);


    @Query("SELECT s FROM OfficerShift s " +
            "WHERE s.officer.station.uid = :stationUid " +
            "AND s.shiftDate = :date " +
            "AND s.isDeleted = false")
    List<OfficerShift> findByStationAndDate(@Param("stationUid") String stationUid, @Param("date") LocalDate date);

    @Query("SELECT s FROM OfficerShift s " +
            "WHERE s.shiftDate = :date " +
            "AND s.isDeleted = false")
    List<OfficerShift> findByDate(@Param("date") LocalDate date);
}
