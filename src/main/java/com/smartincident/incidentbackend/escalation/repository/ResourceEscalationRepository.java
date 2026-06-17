package com.smartincident.incidentbackend.escalation.repository;

import com.smartincident.incidentbackend.enums.ResourceEscalationStatus;
import com.smartincident.incidentbackend.escalation.entity.ResourceEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceEscalationRepository extends JpaRepository<ResourceEscalation, Long> {

    Optional<ResourceEscalation> findByUid(String uid);

    List<ResourceEscalation> findByIncidentUidOrderByCreatedAtDesc(String incidentUid);

    List<ResourceEscalation> findByStatusAndIsActiveTrue(ResourceEscalationStatus status);
}
