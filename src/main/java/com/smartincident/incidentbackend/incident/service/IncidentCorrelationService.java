package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.CorrelationType;
import com.smartincident.incidentbackend.incident.dto.CorrelationResult;
import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.entity.IncidentCorrelation;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentCorrelationRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Incident Correlation Engine.
 *
 * <p>Runs before every new incident is persisted. Searches for active incidents
 * within a configurable radius and time window and scores each candidate using a
 * composite rule set:
 *
 * <pre>
 *  Distance ≤ 100 m                 → +50
 *  Distance ≤ 200 m                 → +30
 *  Distance ≤ 300 m (search limit)  → +15
 *  Time delta ≤ 10 min              → +25
 *  Time delta ≤ 20 min              → +20
 *  Time delta ≤ 30 min              → +15
 *  Emergency category match         → +15
 *  Incident type match              → +10
 * </pre>
 *
 * <ul>
 *   <li>Score ≥ 80 → {@code DUPLICATE}: reporter attached to master; no new incident created.</li>
 *   <li>Score ≥ 50 → {@code RELATED}: new incident created; cross-link recorded.</li>
 *   <li>Score  &lt; 50 → independent event; normal creation flow.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentCorrelationService {

    // ── Tuneable thresholds ──────────────────────────────────────────────────
    /** Search radius for candidate incidents (km). */
    private static final double SEARCH_RADIUS_KM   = 0.3;
    /** Maximum age of an active incident to consider as a candidate (minutes). */
    private static final int    WINDOW_MINUTES      = 30;
    /** Minimum score to classify as DUPLICATE. */
    private static final double DUPLICATE_THRESHOLD = 80.0;
    /** Minimum score to classify as RELATED. */
    private static final double RELATED_THRESHOLD   = 50.0;
    /** Earth radius in metres (Haversine). */
    private static final double EARTH_RADIUS_M      = 6_371_000.0;

    private final IncidentReportRepository      incidentRepository;
    private final IncidentCorrelationRepository correlationRepository;
    private final NotificationService           notificationService;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Runs the correlation algorithm against incoming DTO.
     * Called by {@code IncidentReportService.createIncident()} before saving.
     *
     * @return {@link CorrelationResult#none()} when no correlation is found,
     *         otherwise DUPLICATE or RELATED result with the best-matching master.
     */
    public CorrelationResult detect(IncidentReportDto dto) {
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            return CorrelationResult.none();
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);

        List<IncidentReport> candidates = incidentRepository.findCandidatesForCorrelation(
                dto.getLatitude(), dto.getLongitude(), SEARCH_RADIUS_KM, since);

        if (candidates.isEmpty()) return CorrelationResult.none();

        record Scored(IncidentReport incident, double score, double distM, long deltaS) {}

        Scored best = candidates.stream()
                .map(c -> {
                    double distM  = haversineMeters(dto.getLatitude(), dto.getLongitude(),
                                                    c.getLatitude(), c.getLongitude());
                    long   deltaS = ChronoUnit.SECONDS.between(c.getReportedAt(), LocalDateTime.now());
                    double score  = score(dto, c, distM, deltaS);
                    return new Scored(c, score, distM, deltaS);
                })
                .max(Comparator.comparingDouble(Scored::score))
                .orElse(null);

        if (best == null || best.score() < RELATED_THRESHOLD) return CorrelationResult.none();

        log.info("Correlation detected: score={} dist={}m incident={}",
                 best.score(), (int) best.distM(), best.incident().getUid());

        if (best.score() >= DUPLICATE_THRESHOLD) {
            return CorrelationResult.duplicate(best.incident(), best.score(),
                                               best.distM(), best.deltaS());
        }
        return CorrelationResult.related(best.incident(), best.score(),
                                         best.distM(), best.deltaS());
    }

    /**
     * Handles a DUPLICATE result: attaches the citizen as a co-reporter on the master
     * incident, appends any new evidence, persists a correlation record, and notifies
     * the citizen that responders are already on the way.
     *
     * @return the master incident wrapped in a Warning response so the caller can
     *         communicate the situation to the citizen.
     */
    @Transactional
    public Response<IncidentReport> linkDuplicate(CorrelationResult result,
                                                   User reporter,
                                                   IncidentReportDto dto) {
        IncidentReport master = result.getMasterIncident();

        // Add reporter to co-reporters list (guard against re-entry)
        if (master.getCoReporters() == null) {
            master.setCoReporters(new java.util.ArrayList<>());
        }
        boolean alreadyListed = master.getCoReporters().stream()
                .anyMatch(u -> u.getUid().equals(reporter.getUid()));
        if (!alreadyListed) {
            master.getCoReporters().add(reporter);
        }

        // Increment correlated report count
        int current = master.getCorrelatedReportCount() != null ? master.getCorrelatedReportCount() : 0;
        master.setCorrelatedReportCount(current + 1);

        // Append evidence URLs from the new report if the master doesn't have them
        if (master.getImageUrl() == null && dto.getImageUrl() != null)
            master.setImageUrl(dto.getImageUrl());
        if (master.getAudioUrl() == null && dto.getAudioUrl() != null)
            master.setAudioUrl(dto.getAudioUrl());
        if (master.getVideoUrl() == null && dto.getVideoUrl() != null)
            master.setVideoUrl(dto.getVideoUrl());

        incidentRepository.save(master);

        // Persist the correlation record
        saveCorrelationRecord(master, null, result, true);

        // Notify the citizen
        notifyCitizenDuplicate(reporter, master);

        log.info("Duplicate incident linked: reporter={} master={} score={}",
                 reporter.getUid(), master.getUid(), result.getConfidenceScore());

        return Response.warning(master,
                "Duplicate detected. Your report has been linked to an existing incident. " +
                "Emergency responders have already been notified.");
    }

    /**
     * Persists a RELATED correlation record after the new incident has been saved.
     * Called by {@code IncidentReportService} for RELATED results.
     */
    @Transactional
    public void recordRelated(IncidentReport newIncident, CorrelationResult result) {
        saveCorrelationRecord(result.getMasterIncident(), newIncident, result, true);
        log.info("Related incidents linked: master={} new={} score={}",
                 result.getMasterIncident().getUid(), newIncident.getUid(),
                 result.getConfidenceScore());
    }

    /**
     * Manually links two incidents as the given type.
     * Intended for dispatcher use (e.g., POST /api/incidents/{uid}/correlations/link/{otherUid}).
     */
    @Transactional
    public Response<IncidentCorrelation> manualLink(IncidentReport master,
                                                     IncidentReport linked,
                                                     CorrelationType type,
                                                     String note) {
        double distM  = haversineMeters(
                master.getLatitude(),  master.getLongitude(),
                linked.getLatitude(),  linked.getLongitude());
        long deltaS = ChronoUnit.SECONDS.between(master.getReportedAt(), linked.getReportedAt());

        IncidentCorrelation c = new IncidentCorrelation();
        c.setMasterIncident(master);
        c.setLinkedIncident(linked);
        c.setCorrelationType(type);
        c.setConfidenceScore(100.0);
        c.setDistanceMeters(distM);
        c.setTimeDeltaSeconds(Math.abs(deltaS));
        c.setAutoLinked(false);
        c.setDetectedAt(LocalDateTime.now());
        c.setDispatcherNote(note);

        if (type == CorrelationType.DUPLICATE && !linked.getIsDuplicate()) {
            linked.setIsDuplicate(true);
            incidentRepository.save(linked);
        }

        return new Response<>(correlationRepository.save(c));
    }

    // ── Scoring algorithm ─────────────────────────────────────────────────────

    private double score(IncidentReportDto dto, IncidentReport candidate,
                         double distM, long deltaS) {
        double score = 0;

        // Location component (max 50 pts)
        if (distM <= 100)      score += 50;
        else if (distM <= 200) score += 30;
        else if (distM <= 300) score += 15;

        // Time component (max 25 pts)
        long deltaMin = deltaS / 60;
        if (deltaMin <= 10)      score += 25;
        else if (deltaMin <= 20) score += 20;
        else if (deltaMin <= 30) score += 15;

        // Category component (max 15 pts)
        if (dto.getEmergencyCategory() != null &&
                dto.getEmergencyCategory() == candidate.getEmergencyCategory())
            score += 15;

        // Type component (max 10 pts)
        if (dto.getType() != null && dto.getType() == candidate.getType())
            score += 10;

        return score;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveCorrelationRecord(IncidentReport master,
                                       IncidentReport linked,
                                       CorrelationResult result,
                                       boolean auto) {
        IncidentCorrelation c = new IncidentCorrelation();
        c.setMasterIncident(master);
        c.setLinkedIncident(linked);
        c.setCorrelationType(result.getCorrelationType());
        c.setConfidenceScore(result.getConfidenceScore());
        c.setDistanceMeters(result.getDistanceMeters());
        c.setTimeDeltaSeconds(result.getTimeDeltaSeconds());
        c.setAutoLinked(auto);
        c.setDetectedAt(LocalDateTime.now());
        correlationRepository.save(c);
    }

    private void notifyCitizenDuplicate(User citizen, IncidentReport master) {
        try {
            NotificationDto n = new NotificationDto();
            n.setTitle("Incident Already Reported");
            n.setMessage("Emergency responders have already been notified about this incident. " +
                         "Your report has been recorded and linked. Stay safe.");
            n.setType(com.smartincident.incidentbackend.enums.NotificationType.INCIDENT_REPORTED);
            n.setTargetUserUids(java.util.List.of(citizen.getUid()));
            n.setRelatedEntityUid(master.getUid());
            n.setRelatedEntityType("INCIDENT");
            notificationService.sendNotification(n);
        } catch (Exception e) {
            log.warn("Failed to send duplicate notification to {}: {}", citizen.getUid(), e.getMessage());
        }
    }

    /**
     * Haversine distance between two GPS points, in metres.
     */
    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
