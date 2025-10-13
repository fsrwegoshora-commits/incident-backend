package com.smartincident.incidentbackend.incident.repository;

import com.smartincident.incidentbackend.incident.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByUid(String uid);

    // Get messages for an incident
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.relatedIncident.uid = :incidentUid " +
            "ORDER BY m.sentAt ASC")
    List<ChatMessage> findByIncident(@Param("incidentUid") String incidentUid);

    // Get paginated messages
    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.relatedIncident.uid = :incidentUid " +
            "ORDER BY m.sentAt DESC")
    Page<ChatMessage> findByIncidentPaginated(
            @Param("incidentUid") String incidentUid,
            Pageable pageable
    );
}