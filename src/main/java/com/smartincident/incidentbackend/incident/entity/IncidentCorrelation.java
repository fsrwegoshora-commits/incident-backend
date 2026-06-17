package com.smartincident.incidentbackend.incident.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.CorrelationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Records the relationship between two IncidentReports detected by the
 * Incident Correlation Engine.
 *
 * <ul>
 *   <li>{@code DUPLICATE} — linkedIncident was filed by a different citizen for the
 *       same real-world event.  The linked incident is suppressed; its reporter is
 *       attached to masterIncident as a co-reporter.</li>
 *   <li>{@code RELATED}   — a nearby incident in the same time window but a different
 *       category or type.  Both incidents remain active; the link provides context.</li>
 * </ul>
 */
@Entity
@Table(
    name = "incident_correlations",
    indexes = {
        @Index(name = "idx_ic_master",  columnList = "master_incident_id"),
        @Index(name = "idx_ic_linked",  columnList = "linked_incident_id"),
        @Index(name = "idx_ic_type",    columnList = "correlation_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCorrelation extends BaseEntity {

    /** The existing (older) incident that is the authoritative record. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_incident_id", nullable = false)
    private IncidentReport masterIncident;

    /**
     * The newly submitted report that was correlated to the master.
     * Null only when a duplicate was detected but the duplicate report was
     * fully merged and not persisted as a separate incident.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_incident_id")
    private IncidentReport linkedIncident;

    @Enumerated(EnumType.STRING)
    @Column(name = "correlation_type", nullable = false, length = 20)
    private CorrelationType correlationType;

    /** Composite score 0-100 assigned by the detection engine. */
    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    /** Straight-line distance in metres between the two incident GPS points. */
    @Column(name = "distance_meters", nullable = false)
    private double distanceMeters;

    /** Time between masterIncident.reportedAt and the correlated report, in seconds. */
    @Column(name = "time_delta_seconds", nullable = false)
    private long timeDeltaSeconds;

    /** True when the engine auto-linked the reports; false when a dispatcher linked them manually. */
    @Column(name = "auto_linked", nullable = false)
    private boolean autoLinked = true;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    /** Optional dispatcher note when the link was manually created or overridden. */
    @Column(name = "dispatcher_note", columnDefinition = "TEXT")
    private String dispatcherNote;
}
