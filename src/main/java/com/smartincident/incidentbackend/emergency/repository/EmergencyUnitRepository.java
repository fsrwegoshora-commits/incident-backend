package com.smartincident.incidentbackend.emergency.repository;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmergencyUnitRepository extends JpaRepository<EmergencyUnit, Long> {

    Optional<EmergencyUnit> findByUid(String uid);

    /** All units belonging to an agency (e.g. all TFRF fire stations). */
    List<EmergencyUnit> findByAgencyUidAndIsActiveTrue(String agencyUid);

    /** All units of a specific type (e.g. all FIRE_DISTRICT_STATIONs). */
    List<EmergencyUnit> findByUnitTypeAndIsActiveTrue(UnitType unitType);

    /** All direct children of a parent unit (hierarchy traversal one level). */
    List<EmergencyUnit> findByParentUnitUidAndIsActiveTrue(String parentUid);

    /** Search by name / contact across all units. */
    @Query("""
        select u from EmergencyUnit u
        where (:key is null or lower(u.name) like %:key%)
          and (:isActive is null or u.isActive = :isActive)
          and (:agencyUid is null or u.agency.uid = :agencyUid)
          and (:unitType is null or u.unitType = :unitType)
          and (:level is null or u.level = :level)
        order by u.name asc
        """)
    Page<EmergencyUnit> search(
            Pageable pageable,
            String key,
            Boolean isActive,
            String agencyUid,
            UnitType unitType,
            UnitLevel level
    );

    /**
     * Find the nearest units to given coordinates within radiusKm,
     * filtered by unit type.
     */
    @Query(value = """
        SELECT *, (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(l.latitude)) *
                cos(radians(l.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(l.latitude))
            )
        ) AS distance
        FROM emergency_units eu
        JOIN locations l ON l.id = eu.location_id
        WHERE eu.is_active = true
          AND (:unitType IS NULL OR eu.unit_type = :unitType)
        HAVING distance <= :radiusKm
        ORDER BY distance ASC
        LIMIT 20
        """, nativeQuery = true)
    List<EmergencyUnit> findNearby(
            double lat,
            double lng,
            double radiusKm,
            String unitType
    );
}
