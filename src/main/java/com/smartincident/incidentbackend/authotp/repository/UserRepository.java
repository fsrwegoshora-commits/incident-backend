package com.smartincident.incidentbackend.authotp.repository;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.Role;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUid(String uid);

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query(value = "select distinct u from User u left join fetch u.station s where lower(concat(u.name,u.phoneNumber)) like %:key% and (:isActive is null or u.isActive=:isActive) and (:stationUid is null or s.uid=:stationUid or exists (select o from PoliceOfficer o where o.userAccount=u and o.station.uid=:stationUid))",
           countQuery = "select count(distinct u) from User u left join u.station s where lower(concat(u.name,u.phoneNumber)) like %:key% and (:isActive is null or u.isActive=:isActive) and (:stationUid is null or s.uid=:stationUid or exists (select o from PoliceOfficer o where o.userAccount=u and o.station.uid=:stationUid))")
    Page<User> findByKey(Pageable pageable, Boolean isActive, String key, String stationUid);

    @Query(value = "select u from User u left join fetch u.station s where lower(concat(u.name,u.phoneNumber)) like %:key% and (:isActive is null or u.isActive=:isActive) and s.uid = :stationUid",
           countQuery = "select count(u) from User u left join u.station s where lower(concat(u.name,u.phoneNumber)) like %:key% and (:isActive is null or u.isActive=:isActive) and s.uid = :stationUid")
    Page<User> getUsersByStation(Pageable pageable, Boolean isActive, String key, String stationUid);

    List<User> findByRoleAndIsActiveTrue(Role role);

    List<User> findByRoleInAndIsActiveTrue(List<Role> allowedRoles);

    List<User> findByRoleAndStationUidAndIsActiveTrue(Role role, String stationUid);

    List<User> findByRoleInAndStationUidAndIsActiveTrue(List<Role> allowedRoles, String stationUid);

  //  boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIsActiveTrue(String cleanedPhoneNumber);

    Optional<User> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.station.uid = :stationUid AND u.isActive=true")
    List<User> findByRoleAndStation(@Param("role") Role role, @Param("stationUid") String stationUid);

    List<User> findByRole(Role targetRole);
}
