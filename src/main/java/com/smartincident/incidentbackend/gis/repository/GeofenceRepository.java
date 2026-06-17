package com.smartincident.incidentbackend.gis.repository;

import com.smartincident.incidentbackend.gis.entity.Geofence;
import com.smartincident.incidentbackend.enums.GeofenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    Optional<Geofence> findByUid(String uid);

    List<Geofence> findByIsActiveTrueOrderByNameAsc();

    List<Geofence> findByAgencyUidAndIsActiveTrue(String agencyUid);

    List<Geofence> findByTypeAndIsActiveTrue(GeofenceType type);

    /** Find all active geofences that contain the given point (Haversine-based radius check). */
    @Query(value = """
        SELECT * FROM geofences g
        WHERE g.is_active = true
          AND (6371000 * acos(
                cos(radians(:lat)) * cos(radians(g.center_latitude))
                * cos(radians(g.center_longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(g.center_latitude))
              )) <= g.radius_metres
        """, nativeQuery = true)
    List<Geofence> findContainingPoint(double lat, double lng);
}
