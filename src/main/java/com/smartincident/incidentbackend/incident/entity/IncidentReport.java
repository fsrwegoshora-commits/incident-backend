package com.smartincident.incidentbackend.incident.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.EmergencyCategory;
import com.smartincident.incidentbackend.enums.EmergencyLevel;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.IncidentType;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.setting.entity.Agency;
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

    /** Medical symptoms described by the citizen (used for MEDICAL category). */
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(nullable = false)
    private Boolean isLiveCallRequested = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.REPORTED;

    /** Which combination of services the citizen requested. */
    @Enumerated(EnumType.STRING)
    @Column
    private EmergencyCategory emergencyCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_agency_id")
    private Agency leadAgency;

    /**
     * The PoliceStation assigned to this incident (set when requiresPoliceService = true).
     * Null for fire-only or medical-only incidents.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_police_station_id")
    private PoliceStation assignedPoliceStation;

    /**
     * The EmergencyUnit (Fire Station or Hospital Ambulance Unit) assigned to this incident.
     * Set when requiresFireService or requiresMedicalService = true.
     * Null for police-only incidents.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_unit_id")
    private EmergencyUnit assignedUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private PoliceOfficer assignedOfficer;

    // ── Multi-agency / major incident fields ────────────────────────────

    /** Overall incident commander (may belong to any agency). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_commander_id")
    private User incidentCommander;

    /** Agency of the incident commander. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_commander_agency_id")
    private Agency incidentCommanderAgency;

    /** True when this is classified as a major / complex incident requiring EOC activation. */
    @Column(nullable = false)
    private Boolean isMajorIncident = false;

    /** True when the Emergency Operations Centre (EOC) has been formally activated. */
    @Column(nullable = false)
    private Boolean eocActivated = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<IncidentAgency> involvedAgencies;

    @OneToMany(mappedBy = "relatedIncident", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ChatMessage> chatMessages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyLevel emergencyLevel;

    @Column(nullable = false)
    private Boolean requiresPoliceService = false;

    @Column(nullable = false)
    private Boolean requiresFireService = false;

    @Column(nullable = false)
    private Boolean requiresMedicalService = false;

    /**
     * The Dispatcher who owns this incident.
     * Auto-assigned at creation time to whichever Dispatcher is currently on duty.
     * Null only when no dispatcher shift is active at the moment of reporting.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_dispatcher_id")
    private User assignedDispatcher;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<com.smartincident.incidentbackend.emergency.entity.IncidentDispatch> dispatches;

    /** Set automatically when status transitions to DISPATCHED. */
    @Column
    private LocalDateTime dispatchedAt;

    /** Set automatically when status transitions to AT_SCENE. */
    @Column
    private LocalDateTime atSceneAt;

    /** Set when a dispatcher escalates to supervisor/DC admin. */
    @Column
    private LocalDateTime escalatedAt;

    @Column(columnDefinition = "TEXT")
    private String escalationNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_by_id")
    private User escalatedBy;

    @Override
    public String toString() {
        return "IncidentReport{" +
                "id=" + getId() +
                ", uid='" + getUid() + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", status=" + status +
                // OMIT chatMessages, reportedBy, assignedStation, etc.
                '}';
    }
}