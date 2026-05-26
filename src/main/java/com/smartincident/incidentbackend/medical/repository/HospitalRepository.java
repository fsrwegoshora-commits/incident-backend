package com.smartincident.incidentbackend.medical.repository;

import com.smartincident.incidentbackend.enums.HospitalType;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Optional<Hospital> findByUid(String uid);

    List<Hospital> findByHospitalTypeAndIsActiveTrue(HospitalType type);

    /** Hospitals that have a registered ambulance unit. */
    @Query("select h from Hospital h where h.ambulanceUnit is not null and h.isActive = true")
    List<Hospital> findWithAmbulanceUnit();

    @Query("""
        select h from Hospital h
        where (:key is null or lower(h.name) like %:key%)
          and (:isActive is null or h.isActive = :isActive)
          and (:hospitalType is null or h.hospitalType = :hospitalType)
        order by h.name asc
        """)
    Page<Hospital> search(Pageable pageable, String key, Boolean isActive, HospitalType hospitalType);

    /**
     * Nearest hospitals with an ambulance unit, within radiusKm.
     */
    @Query(value = """
        SELECT h.*, (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(l.latitude)) *
                cos(radians(l.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(l.latitude))
            )
        ) AS distance
        FROM hospitals h
        JOIN locations l ON l.id = h.location_id
        WHERE h.is_active = true
          AND h.ambulance_unit_id IS NOT NULL
        HAVING distance <= :radiusKm
        ORDER BY distance ASC
        LIMIT 10
        """, nativeQuery = true)
    List<Hospital> findNearestWithAmbulance(double lat, double lng, double radiusKm);
}
