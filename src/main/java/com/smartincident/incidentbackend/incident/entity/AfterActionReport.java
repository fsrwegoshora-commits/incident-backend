package com.smartincident.incidentbackend.incident.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "after_action_reports", indexes = {
        @Index(name = "idx_aar_incident", columnList = "incident_id"),
        @Index(name = "idx_aar_author", columnList = "authored_by_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfterActionReport extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private IncidentReport incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authored_by_id", nullable = false)
    private User authoredBy;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String executiveSummary;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @Column(columnDefinition = "TEXT")
    private String whatWentWell;

    @Column(columnDefinition = "TEXT")
    private String whatWentWrong;

    @Column(columnDefinition = "TEXT")
    private String rootCauseAnalysis;

    @Column(columnDefinition = "TEXT")
    private String recommendedActions;

    @Column(columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(columnDefinition = "TEXT")
    private String agencyCoordinationNotes;

    @Column
    private Long responseTimeMinutes;

    @Column
    private Long dispatchToArrivalMinutes;

    @Column
    private Long totalResolutionMinutes;

    @Column
    private Boolean slaBreached;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;
}
