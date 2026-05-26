package com.smartincident.incidentbackend.dispatcher.repository;

import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DispatcherShiftRepository extends JpaRepository<DispatcherShift, Long> {

    Optional<DispatcherShift> findByUid(String uid);

    List<DispatcherShift> findByShiftDateAndIsDeletedFalse(LocalDate date);

    List<DispatcherShift> findByDispatcherUidAndIsDeletedFalse(String dispatcherUid);

    boolean existsByDispatcherUidAndShiftDateAndIsDeletedFalse(String dispatcherUid, LocalDate date);

    /** All shifts active right now (today, start ≤ now ≤ end). */
    @Query("SELECT s FROM DispatcherShift s " +
           "WHERE s.shiftDate = :today " +
           "AND s.startTime <= :now AND s.endTime >= :now " +
           "AND s.isDeleted = false")
    List<DispatcherShift> findOnDutyNow(@Param("today") LocalDate today,
                                         @Param("now") LocalTime now);

    /** All shifts for dispatchers belonging to a given dispatch center. */
    @Query("SELECT s FROM DispatcherShift s " +
           "WHERE s.dispatcher.emergencyUnit.uid = :centerUid " +
           "AND s.isDeleted = false " +
           "ORDER BY s.shiftDate DESC, s.startTime ASC")
    List<DispatcherShift> findByDispatchCenterUid(@Param("centerUid") String centerUid);
}
