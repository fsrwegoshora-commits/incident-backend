package com.smartincident.incidentbackend.police.repository;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PoliceOfficerRepository extends JpaRepository<PoliceOfficer,Long> {

    Optional<PoliceOfficer> findByUid(String uid);

    @Query("""
    select p from PoliceOfficer p
    where lower(concat(p.userAccount.name, p.userAccount.phoneNumber)) like %:key%
      and (:isActive is null or p.isActive = :isActive) and (:stationUid is null or p.station.uid=:stationUid)
    """)
    Page<PoliceOfficer> getPoliceOfficers(Pageable pageable, Boolean isActive, String key,String stationUid);

    @Query("select p from PoliceOfficer p where lower(concat(p.userAccount.name, p.userAccount.phoneNumber)) like %:key% and (:isActive is null or p.isActive=:isActive) and p.station.uid=:stationUid")
    Page<PoliceOfficer> getPoliceOfficersByStation(Pageable pageable, Boolean isActive, String key, String stationUid);

    @Query("select p from PoliceOfficer p where p.userAccount.uid = :userUid and p.isActive=true")
    Optional<PoliceOfficer> findByUserUidAndIsActiveTrue(String userUid);

    boolean existsByUserAccount(User user);

    List<PoliceOfficer> findByIsActiveTrue();

    List<PoliceOfficer> findByStationUidAndIsActiveTrue(String stationUid);
}
