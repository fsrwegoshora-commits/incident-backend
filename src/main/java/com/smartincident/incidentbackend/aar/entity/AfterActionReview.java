package com.smartincident.incidentbackend.aar.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "after_action_reviews", indexes = {
    @Index(name = "idx_aar_incident", columnList = "incident_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AfterActionReview extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private IncidentReport incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conducted_by_id")
    private User conductedBy;

    @Column(nullable = false)
    private LocalDateTime conductedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "what_worked", columnDefinition = "TEXT")
    private String whatWorked;

    @Column(name = "areas_for_improvement", columnDefinition = "TEXT")
    private String areasForImprovement;

    @Column(name = "recommended_improvements", columnDefinition = "TEXT")
    private String recommendedImprovements;

    /** Minutes from reportedAt → dispatchedAt. */
    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    /** Minutes from dispatchedAt → atSceneAt. */
    @Column(name = "at_scene_time_minutes")
    private Integer atSceneTimeMinutes;

    /** Minutes from reportedAt → resolvedAt. */
    @Column(name = "resolution_time_minutes")
    private Integer resolutionTimeMinutes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean slaBreached = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column
    private LocalDateTime approvedAt;
}
