package com.smartincident.incidentbackend.incident.entity;

import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.utils.Utils;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_status_history", indexes = {
        @Index(name = "idx_ish_incident", columnList = "incident_id"),
        @Index(name = "idx_ish_timestamp", columnList = "changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, length = 50)
    private String uid = Utils.generateUniqueID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentReport incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private IncidentStatus toStatus;

    @Column(name = "changed_by_uid", length = 50)
    private String changedByUid;

    @Column(name = "changed_by_name", length = 150)
    private String changedByName;

    @Column(name = "changed_by_role", length = 50)
    private String changedByRole;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String note;
}
