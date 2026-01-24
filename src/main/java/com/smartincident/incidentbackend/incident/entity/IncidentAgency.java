package com.smartincident.incidentbackend.incident.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.AgencyResponseStatus;
import com.smartincident.incidentbackend.enums.AgencyRole;
import com.smartincident.incidentbackend.setting.entity.Agency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_agencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAgency extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentReport incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgencyRole agencyRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgencyResponseStatus responseStatus = AgencyResponseStatus.NOTIFIED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_officer_id")
    private User responsibleOfficer;

    @Column(nullable = false)
    private LocalDateTime notifiedAt = LocalDateTime.now();

    @Column
    private LocalDateTime respondedAt;

    @Column
    private LocalDateTime arrivedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String statusNotes; // e.g., "Fire truck on the way, ETA 5 mins"

    @Column(columnDefinition = "TEXT")
    private String completionNotes; // Final report
}
