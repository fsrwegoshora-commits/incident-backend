package com.smartincident.incidentbackend.escalation.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.EscalationLevel;
import com.smartincident.incidentbackend.enums.ResourceEscalationStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_escalations", indexes = {
    @Index(name = "idx_res_esc_incident", columnList = "incident_id"),
    @Index(name = "idx_res_esc_status",   columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceEscalation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentReport incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResourceEscalationStatus status = ResourceEscalationStatus.PENDING;

    @Column(nullable = false, length = 50)
    private String requestedByUid;

    @Column(length = 150)
    private String requestedByName;

    @Column(length = 30)
    private String requestedByRole;

    /** Which service is unavailable: POLICE, FIRE, MEDICAL, MULTI */
    @Column(length = 20)
    private String resourceType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String responseNotes;

    @Column(length = 50)
    private String respondedByUid;

    @Column(length = 150)
    private String respondedByName;

    @Column
    private LocalDateTime respondedAt;
}
