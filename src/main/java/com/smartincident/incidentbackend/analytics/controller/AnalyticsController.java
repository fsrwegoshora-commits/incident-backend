package com.smartincident.incidentbackend.analytics.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final IncidentReportRepository incidentRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * GET /api/analytics/incidents
     * Returns summary, breakdown by type, breakdown by status, and 30-day daily trend.
     * Scope: ROOT → all agencies; AGENCY_ADMIN → own agency; others → own agency too.
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_ANALYTICS)
    @GetMapping("/incidents")
    public Response<Map<String, Object>> getIncidentAnalytics(
            @RequestParam(required = false) Integer days) {

        String agencyUid = resolveAgencyScope();
        int trendDays = (days != null && days > 0 && days <= 365) ? days : 30;
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays);

        // Summary counts
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total",      incidentRepo.countTotal(agencyUid));
        summary.put("pending",    incidentRepo.countByStatus(IncidentStatus.REPORTED, agencyUid)
                                + incidentRepo.countByStatus(IncidentStatus.PENDING, agencyUid)
                                + incidentRepo.countByStatus(IncidentStatus.UNDER_REVIEW, agencyUid)
                                + incidentRepo.countByStatus(IncidentStatus.CLASSIFIED, agencyUid)
                                + incidentRepo.countByStatus(IncidentStatus.WAITING_FOR_DISPATCH, agencyUid));
        summary.put("inProgress", incidentRepo.countByStatus(IncidentStatus.IN_PROGRESS, agencyUid));
        summary.put("resolved",   incidentRepo.countByStatus(IncidentStatus.RESOLVED, agencyUid));
        summary.put("closed",     incidentRepo.countByStatus(IncidentStatus.CLOSED, agencyUid));
        summary.put("rejected",   incidentRepo.countByStatus(IncidentStatus.REJECTED, agencyUid));

        // Breakdown by incident type
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : incidentRepo.countGroupedByType(agencyUid)) {
            byType.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        // 30-day daily trend
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : incidentRepo.countGroupedByDay(since, agencyUid)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date",  row[0].toString());
            point.put("count", ((Number) row[1]).longValue());
            trend.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("byType",  byType);
        result.put("trend",   trend);
        result.put("trendDays", trendDays);

        return Response.success(result);
    }

    /**
     * GET /api/analytics/performance
     * Dispatcher performance metrics: avg dispatch time, avg response time, avg resolution time,
     * dispatcher workload, and escalation count.
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_ANALYTICS)
    @GetMapping("/performance")
    public Response<Map<String, Object>> getPerformanceAnalytics() {
        String agencyUid = resolveAgencyScope();

        // Fetch dispatched incidents for time-based metrics
        List<com.smartincident.incidentbackend.incident.entity.IncidentReport> dispatched =
                incidentRepo.findDispatchedIncidents(agencyUid);

        // Average dispatch time: REPORTED → DISPATCHED
        OptionalDouble avgDispatch = dispatched.stream()
                .filter(i -> i.getReportedAt() != null && i.getDispatchedAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getReportedAt(), i.getDispatchedAt()).toMinutes())
                .filter(m -> m >= 0)
                .average();

        // Average at-scene time: DISPATCHED → AT_SCENE
        OptionalDouble avgAtScene = dispatched.stream()
                .filter(i -> i.getDispatchedAt() != null && i.getAtSceneAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getDispatchedAt(), i.getAtSceneAt()).toMinutes())
                .filter(m -> m >= 0)
                .average();

        // Average resolution time: DISPATCHED → RESOLVED
        OptionalDouble avgResolution = dispatched.stream()
                .filter(i -> i.getDispatchedAt() != null && i.getResolvedAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getDispatchedAt(), i.getResolvedAt()).toMinutes())
                .filter(m -> m >= 0)
                .average();

        // Dispatcher workload
        List<Map<String, Object>> workload = new ArrayList<>();
        for (Object[] row : incidentRepo.findDispatcherWorkload()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("dispatcherUid",  row[0]);
            entry.put("dispatcherName", row[1]);
            entry.put("totalIncidents", ((Number) row[2]).longValue());
            entry.put("activeIncidents", ((Number) row[3]).longValue());
            workload.add(entry);
        }

        // Full status breakdown
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (com.smartincident.incidentbackend.enums.IncidentStatus s :
                com.smartincident.incidentbackend.enums.IncidentStatus.values()) {
            long count = incidentRepo.countByStatus(s, agencyUid);
            if (count > 0) statusBreakdown.put(s.name(), count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgDispatchMinutes",   avgDispatch.isPresent()   ? Math.round(avgDispatch.getAsDouble())   : null);
        result.put("avgAtSceneMinutes",    avgAtScene.isPresent()    ? Math.round(avgAtScene.getAsDouble())    : null);
        result.put("avgResolutionMinutes", avgResolution.isPresent() ? Math.round(avgResolution.getAsDouble()) : null);
        result.put("dispatcherWorkload",   workload);
        result.put("statusBreakdown",      statusBreakdown);
        result.put("escalatedCount",       incidentRepo.countEscalated(agencyUid));
        result.put("totalDispatched",      (long) dispatched.size());

        return Response.success(result);
    }

    /** ROOT sees all; everyone else is scoped to their agency. */
    private static String resolveAgencyScope() {
        if (LoggedUser.isRoot()) return null;
        return LoggedUser.getAgencyUid();
    }
}
