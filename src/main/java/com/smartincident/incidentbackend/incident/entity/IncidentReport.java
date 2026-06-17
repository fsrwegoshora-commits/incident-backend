package com.smartincident.incidentbackend.incident.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.operational.entity.OperationalPost;
import com.smartincident.incidentbackend.enums.EmergencyCategory;
import com.smartincident.incidentbackend.enums.EmergencyLevel;
import com.smartincident.incidentbackend.enums.IncidentNature;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.IncidentType;
import com.smartincident.incidentbackend.enums.ReportLanguage;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.setting.entity.Agency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_status_active", columnList = "status, is_active"),
        @Index(name = "idx_incidents_station_active", columnList = "assigned_police_station_id, is_active"),
        @Index(name = "idx_incidents_unit_active", columnList = "assigned_unit_id, is_active"),
        @Index(name = "idx_incidents_dispatcher_active", columnList = "assigned_dispatcher_id, is_active"),
        @Index(name = "idx_incidents_reported_at", columnList = "reported_at"),
        @Index(name = "idx_incidents_location", columnList = "latitude, longitude")
})
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

    /** Whether this is an active emergency or a non-emergency (historical/investigative) report. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nature")
    private IncidentNature nature = IncidentNature.EMERGENCY;

    /** Language detected in the report text. */
    @Enumerated(EnumType.STRING)
    @Column(name = "detected_language")
    private ReportLanguage detectedLanguage;

    /** One-sentence reasoning produced by the AI triage engine. */
    @Column(name = "triage_reasoning", columnDefinition = "TEXT")
    private String triageReasoning;

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

    /** The operational post (patrol/fire/medical deployment) that responded to this incident. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operational_post_id")
    private OperationalPost assignedOperationalPost;

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

    /** Set automatically when status transitions to ACKNOWLEDGED. */
    @Column
    private LocalDateTime acknowledgedAt;

    /** Set automatically when status transitions to EN_ROUTE. */
    @Column
    private LocalDateTime enRouteAt;

    /** Set automatically when status transitions to AT_SCENE. */
    @Column
    private LocalDateTime atSceneAt;

    /** Set automatically when status transitions to CLOSED. */
    @Column
    private LocalDateTime closedAt;

    /** SLA deadline — computed from emergencyLevel at creation time. */
    @Column
    private LocalDateTime slaDeadline;

    /** Set when the SLA deadline is first missed. */
    @Column
    private LocalDateTime slaBreachedAt;

    /** Set when a dispatcher escalates to supervisor/DC admin. */
    @Column
    private LocalDateTime escalatedAt;

    @Column(columnDefinition = "TEXT")
    private String escalationNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_by_id")
    private User escalatedBy;

    // ── Incident Closure Review ────────────────────────────────────────────

    /** Station Admin who reviewed the resolved incident before closure. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_reviewed_by_id")
    private User stationReviewedBy;

    @Column
    private LocalDateTime stationReviewedAt;

    @Column(columnDefinition = "TEXT")
    private String stationReviewNotes;

    /** Dispatcher who reviewed the station-approved incident before final close. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatcher_reviewed_by_id")
    private User dispatcherReviewedBy;

    @Column
    private LocalDateTime dispatcherReviewedAt;

    @Column(columnDefinition = "TEXT")
    private String dispatcherReviewNotes;

    /** True once the After Action Review has been submitted. */
    @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean aarCompleted = false;

    // ── Incident Correlation ────────────────────────────────────────────────

    /**
     * True when this report was identified as a duplicate of an existing incident
     * and was merged into masterIncident rather than creating a standalone record.
     */
    @Column(nullable = false)
    private Boolean isDuplicate = false;

    /**
     * The authoritative master incident that this report was merged into.
     * Null for master incidents and for independent incidents.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_incident_id")
    private IncidentReport masterIncident;

    /** Running count of duplicate reports merged into this master incident. */
    @Column(name = "correlated_report_count")
    private Integer correlatedReportCount = 0;

    /**
     * Additional citizens who reported the same real-world event.
     * Populated when duplicate detection merges subsequent reports into this master.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name  = "incident_co_reporters",
        joinColumns        = @JoinColumn(name = "incident_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    private List<User> coReporters = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<IncidentStatusHistory> statusHistory = new ArrayList<>();

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