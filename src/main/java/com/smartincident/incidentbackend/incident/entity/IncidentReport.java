package com.smartincident.incidentbackend.incident.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.IncidentType;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentReport extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Column(nullable = false)
    private String location;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private String imageUrl;

    @Column
    private String audioUrl;

    @Column
    private String videoUrl;

    @Column(nullable = false)
    private boolean isLiveCallRequested = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "police_station_id", nullable = false)
    private PoliceStation assignedStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private PoliceOfficer assignedOfficer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "relatedIncident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages;
}