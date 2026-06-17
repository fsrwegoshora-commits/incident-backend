package com.smartincident.incidentbackend.authotp.repository;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.Role;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUid(String uid);

    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Fetches the user with agency/emergencyUnit/policeStation eagerly initialized.
     * Used to cache the authenticated User for the duration of a request (see JwtAuthFilter /
     * LoggedUser) — those lazy associations would otherwise throw LazyInitializationException
     * once the short-lived session this query runs in is closed.
     */
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.agency " +
           "LEFT JOIN FETCH u.emergencyUnit " +
           "LEFT JOIN FETCH u.policeStation " +
           "WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumberWithAssociations(@Param("phoneNumber") String phoneNumber);

    @Query(value = """
        select distinct u from User u
        left join fetch u.emergencyUnit eu
        left join fetch u.policeStation ps
        where lower(concat(u.name, u.phoneNumber)) like %:key%
          and (:isActive is null or u.isActive = :isActive)
          and (:unitUid is null
               or eu.uid = :unitUid
               or ps.uid = :unitUid)
        """,
        countQuery = """
        select count(distinct u) from User u
        left join u.emergencyUnit eu
        left join u.policeStation ps
        where lower(concat(u.name, u.phoneNumber)) like %:key%
          and (:isActive is null or u.isActive = :isActive)
          and (:unitUid is null
               or eu.uid = :unitUid
               or ps.uid = :unitUid)
        """)
    Page<User> findByKey(Pageable pageable, Boolean isActive, String key, String unitUid);

    @Query(value = """
        select u from User u
        left join u.emergencyUnit eu
        left join u.policeStation ps
        where lower(concat(u.name, u.phoneNumber)) like %:key%
          and (:isActive is null or u.isActive = :isActive)
          and (eu.uid = :unitUid or ps.uid = :unitUid)
        """,
        countQuery = """
        select count(u) from User u
        left join u.emergencyUnit eu
        left join u.policeStation ps
        where lower(concat(u.name, u.phoneNumber)) like %:key%
          and (:isActive is null or u.isActive = :isActive)
          and (eu.uid = :unitUid or ps.uid = :unitUid)
        """)
    Page<User> getUsersByStation(Pageable pageable, Boolean isActive, String key, String unitUid);

    List<User> findByRoleAndIsActiveTrue(Role role);

    List<User> findByRoleInAndIsActiveTrue(List<Role> allowedRoles);

    List<User> findByRoleAndEmergencyUnitUidAndIsActiveTrue(Role role, String unitUid);

    List<User> findByRoleInAndEmergencyUnitUidAndIsActiveTrue(List<Role> allowedRoles, String unitUid);

    /** @deprecated use findByRoleAndEmergencyUnitUidAndIsActiveTrue */
    @Deprecated
    default List<User> findByRoleAndStationUidAndIsActiveTrue(Role role, String unitUid) {
        return findByRoleAndEmergencyUnitUidAndIsActiveTrue(role, unitUid);
    }

    /** @deprecated use findByRoleInAndEmergencyUnitUidAndIsActiveTrue */
    @Deprecated
    default List<User> findByRoleInAndStationUidAndIsActiveTrue(List<Role> roles, String unitUid) {
        return findByRoleInAndEmergencyUnitUidAndIsActiveTrue(roles, unitUid);
    }

    boolean existsByPhoneNumberAndIsActiveTrue(String phoneNumber);

    Optional<User> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.emergencyUnit.uid = :unitUid AND u.isActive = true")
    List<User> findByRoleAndStation(@Param("role") Role role, @Param("unitUid") String unitUid);

    /** Find police users (STATION_ADMIN / POLICE_OFFICER) by their police station. */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.policeStation.uid = :stationUid AND u.isActive = true")
    List<User> findByRoleAndPoliceStation(@Param("role") Role role, @Param("stationUid") String stationUid);

    /** Find users of multiple roles at a police station (e.g. STATION_ADMIN + POLICE_OFFICER). */
    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.policeStation.uid = :stationUid AND u.isActive = true")
    List<User> findByRolesAndPoliceStation(@Param("roles") List<Role> roles, @Param("stationUid") String stationUid);

    List<User> findByRole(Role targetRole);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :newRole WHERE u.role = :oldRole")
    int migrateRole(@Param("oldRole") Role oldRole, @Param("newRole") Role newRole);

    List<User> findByAgencyUidAndIsActiveTrue(String agencyUid);

    List<User> findByAgencyUidAndRoleAndIsActiveTrue(String agencyUid, Role role);

    @Query(value = """
        select distinct u from User u
        left join fetch u.emergencyUnit eu
        left join fetch u.policeStation ps
        where lower(concat(u.name, u.phoneNumber)) like %:key%
          and (:isActive is null or u.isActive = :isActive)
          and u.agency.uid = :agencyUid
        """,
        countQuery = """
        select count(distinct u) from User u
        left join u.emergencyUnit eu
        left join u.policeStation ps
        where lower(concat(u.name, u.phoneNumber)) like %:key%
          and (:isActive is null or u.isActive = :isActive)
          and u.agency.uid = :agencyUid
        """)
    Page<User> findByKeyAndAgency(Pageable pageable, Boolean isActive, String key,
                                  @Param("agencyUid") String agencyUid);
}
