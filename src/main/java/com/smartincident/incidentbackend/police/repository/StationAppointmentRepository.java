package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.enums.AppointmentStatus;
import com.smartincident.incidentbackend.police.entity.StationAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StationAppointmentRepository extends JpaRepository<StationAppointment, Long> {

    Optional<StationAppointment> findByUid(String uid);

    List<StationAppointment> findByOfficerUidAndStatusAndIsActiveTrue(String officerUid, AppointmentStatus status);

    @Query("""
        select a from StationAppointment a
        where a.policeStation.uid = :stationUid and a.isActive = true
        order by case a.position
          when 'OFFICER_IN_CHARGE'        then 1
          when 'DEPUTY_OFFICER_IN_CHARGE' then 2
          when 'INVESTIGATOR'             then 3
          when 'DETECTIVE'                then 4
          when 'TRAFFIC_OFFICER'          then 5
          when 'PATROL_OFFICER'           then 6
          when 'ADMINISTRATIVE_OFFICER'   then 7
          when 'COMMUNITY_LIAISON_OFFICER' then 8
          when 'GENERAL_DUTY_OFFICER'     then 9
          else 10 end asc
        """)
    List<StationAppointment> findByStationUidOrderByPosition(String stationUid);

    @Query("""
        select a from StationAppointment a
        where a.policeStation.uid = :stationUid
          and (:status is null or a.status = :status)
          and a.isActive = true
        order by case a.position
          when 'OFFICER_IN_CHARGE'        then 1
          when 'DEPUTY_OFFICER_IN_CHARGE' then 2
          when 'INVESTIGATOR'             then 3
          when 'DETECTIVE'                then 4
          when 'TRAFFIC_OFFICER'          then 5
          when 'PATROL_OFFICER'           then 6
          when 'ADMINISTRATIVE_OFFICER'   then 7
          when 'COMMUNITY_LIAISON_OFFICER' then 8
          when 'GENERAL_DUTY_OFFICER'     then 9
          else 10 end asc
        """)
    Page<StationAppointment> getByStation(Pageable pageable, String stationUid, AppointmentStatus status);

    @Query("""
        select a from StationAppointment a
        where a.officer.uid = :officerUid and a.isActive = true
        order by a.startDate desc
        """)
    List<StationAppointment> findByOfficerUid(String officerUid);

    /** UIDs of officers at a station whose active appointment position is one of the given positions. */
    @Query("""
        select a.officer.uid from StationAppointment a
        where a.policeStation.uid = :stationUid
          and a.position in :positions
          and a.status = com.smartincident.incidentbackend.enums.AppointmentStatus.ACTIVE
          and a.isActive = true
        """)
    List<String> findOfficerUidsByStationAndPositions(String stationUid,
            List<com.smartincident.incidentbackend.enums.AppointmentPosition> positions);
}
