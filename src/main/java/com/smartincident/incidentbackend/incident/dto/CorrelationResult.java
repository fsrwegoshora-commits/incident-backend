package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.CorrelationType;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import lombok.Getter;

/**
 * Immutable value object returned by {@code IncidentCorrelationService.detect()}.
 */
@Getter
public final class CorrelationResult {

    private final boolean matched;
    private final CorrelationType correlationType;   // null when matched = false
    private final IncidentReport masterIncident;     // null when matched = false
    private final double confidenceScore;
    private final double distanceMeters;
    private final long timeDeltaSeconds;

    private CorrelationResult(boolean matched, CorrelationType type,
                              IncidentReport master, double score,
                              double distM, long deltaS) {
        this.matched        = matched;
        this.correlationType = type;
        this.masterIncident  = master;
        this.confidenceScore = score;
        this.distanceMeters  = distM;
        this.timeDeltaSeconds = deltaS;
    }

    /** No correlation found — create a new independent incident. */
    public static CorrelationResult none() {
        return new CorrelationResult(false, null, null, 0, 0, 0);
    }

    /** Strong match — the new report is a duplicate of {@code master}. */
    public static CorrelationResult duplicate(IncidentReport master, double score,
                                              double distM, long deltaS) {
        return new CorrelationResult(true, CorrelationType.DUPLICATE, master, score, distM, deltaS);
    }

    /** Weak match — create a new incident but record a RELATED link to {@code master}. */
    public static CorrelationResult related(IncidentReport master, double score,
                                            double distM, long deltaS) {
        return new CorrelationResult(true, CorrelationType.RELATED, master, score, distM, deltaS);
    }

    public boolean isDuplicate() {
        return matched && correlationType == CorrelationType.DUPLICATE;
    }

    public boolean isRelated() {
        return matched && correlationType == CorrelationType.RELATED;
    }
}
