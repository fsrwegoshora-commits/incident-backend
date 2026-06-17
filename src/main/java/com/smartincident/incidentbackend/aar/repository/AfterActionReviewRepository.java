package com.smartincident.incidentbackend.aar.repository;

import com.smartincident.incidentbackend.aar.entity.AfterActionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AfterActionReviewRepository extends JpaRepository<AfterActionReview, Long> {

    Optional<AfterActionReview> findByUid(String uid);

    Optional<AfterActionReview> findByIncidentUid(String incidentUid);

    boolean existsByIncidentUid(String incidentUid);
}
