package com.smartincident.incidentbackend.operational.repository;

import com.smartincident.incidentbackend.enums.PostStatus;
import com.smartincident.incidentbackend.enums.PostType;
import com.smartincident.incidentbackend.operational.entity.OperationalPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperationalPostRepository extends JpaRepository<OperationalPost, Long> {

    Optional<OperationalPost> findByUid(String uid);

    List<OperationalPost> findByIsActiveTrue();

    List<OperationalPost> findByPostTypeAndStatusAndIsActiveTrue(PostType postType, PostStatus status);

    List<OperationalPost> findByAgencyUidAndIsActiveTrue(String agencyUid);

    List<OperationalPost> findByParentStationUidAndIsActiveTrue(String stationUid);

    List<OperationalPost> findByParentUnitUidAndIsActiveTrue(String unitUid);

    List<OperationalPost> findByParentHospitalUidAndIsActiveTrue(String hospitalUid);

    /**
     * Haversine-based nearby search for operational posts.
     * Returns posts within radiusKm of the given coordinates, ordered by distance.
     */
    @Query(value = """
        SELECT op.*, (
            6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(op.latitude))
                * COS(RADIANS(op.longitude) - RADIANS(:lng))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(op.latitude))
            )
        ) AS distance
        FROM operational_posts op
        WHERE op.is_active = true
          AND op.is_deleted = false
          AND op.status = 'ACTIVE'
          AND op.post_type = :postType
          AND op.latitude IS NOT NULL
          AND op.longitude IS NOT NULL
          AND (
            6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(op.latitude))
                * COS(RADIANS(op.longitude) - RADIANS(:lng))
                + SIN(RADIANS(:lat)) * SIN(RADIANS(op.latitude))
            )
          ) <= :radiusKm
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<OperationalPost> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("postType") String postType);
}
