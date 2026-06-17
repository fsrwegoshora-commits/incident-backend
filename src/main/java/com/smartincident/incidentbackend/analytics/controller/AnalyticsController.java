package com.smartincident.incidentbackend.analytics.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
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
    private final EmergencyVehicleRepository vehicleRepo;
    private final AgencyRepository agencyRepo;

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

    // ── Vehicle Intelligence (Improvement 2) ─────────────────────────────────

    /**
     * Fleet status overview — vehicle counts by status and type.
     * Useful for resource readiness assessment.
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_ANALYTICS)
    @GetMapping("/vehicles")
    public Response<Map<String, Object>> getVehicleIntelligence(
            @RequestParam(required = false) String agencyUid) {

        String scopedAgencyUid = LoggedUser.isRoot() ? agencyUid : LoggedUser.getAgencyUid();

        List<EmergencyVehicle> vehicles = vehicleRepo.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .filter(v -> scopedAgencyUid == null
                        || (v.getEmergencyUnit() != null
                            && v.getEmergencyUnit().getAgency() != null
                            && scopedAgencyUid.equals(v.getEmergencyUnit().getAgency().getUid())))
                .toList();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (VehicleStatus s : VehicleStatus.values()) {
            long count = vehicles.stream().filter(v -> v.getStatus() == s).count();
            if (count > 0) byStatus.put(s.name(), count);
        }

        Map<String, Long> byType = new LinkedHashMap<>();
        for (VehicleType t : VehicleType.values()) {
            long count = vehicles.stream().filter(v -> v.getVehicleType() == t).count();
            if (count > 0) byType.put(t.name(), count);
        }

        long withGps = vehicles.stream()
                .filter(v -> v.getLatitude() != null && v.getLongitude() != null).count();

        List<Map<String, Object>> details = vehicles.stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("uid", v.getUid());
                    m.put("plateNumber", v.getPlateNumber());
                    m.put("model", v.getModel());
                    m.put("vehicleType", v.getVehicleType() != null ? v.getVehicleType().name() : null);
                    m.put("status", v.getStatus().name());
                    m.put("unitName", v.getEmergencyUnit() != null ? v.getEmergencyUnit().getName() : null);
                    m.put("agencyType", v.getEmergencyUnit() != null && v.getEmergencyUnit().getAgency() != null
                            ? v.getEmergencyUnit().getAgency().getAgencyType() : null);
                    m.put("hasGps", v.getLatitude() != null);
                    m.put("latitude", v.getLatitude());
                    m.put("longitude", v.getLongitude());
                    m.put("lastLocationUpdate", v.getLastLocationUpdate());
                    m.put("hasAls", v.getHasAdvancedLifeSupport());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", (long) vehicles.size());
        result.put("withGpsTracking", withGps);
        result.put("byStatus", byStatus);
        result.put("byType", byType);
        result.put("vehicles", details);
        return Response.success(result);
    }

    // ── National Operational Intelligence (Improvement 8) ─────────────────────

    /**
     * Nationwide analytics — response time trends, agency performance comparison,
     * incident type distribution. ROOT only.
     */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN})
    @RequiresPermission(Permission.VIEW_ANALYTICS)
    @GetMapping("/national")
    public Response<Map<String, Object>> getNationalAnalytics(
            @RequestParam(defaultValue = "30") int days) {

        if (days < 1 || days > 365) days = 30;
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // All incidents in the window
        List<com.smartincident.incidentbackend.incident.entity.IncidentReport> all =
                incidentRepo.findAll().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getIsActive())
                                && i.getReportedAt() != null && i.getReportedAt().isAfter(since))
                        .toList();

        // Response time stats
        OptionalDouble avgDispatch = all.stream()
                .filter(i -> i.getReportedAt() != null && i.getDispatchedAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getReportedAt(), i.getDispatchedAt()).toMinutes())
                .filter(m -> m >= 0).average();

        OptionalDouble avgAtScene = all.stream()
                .filter(i -> i.getDispatchedAt() != null && i.getAtSceneAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getDispatchedAt(), i.getAtSceneAt()).toMinutes())
                .filter(m -> m >= 0).average();

        OptionalDouble avgResolution = all.stream()
                .filter(i -> i.getDispatchedAt() != null && i.getResolvedAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getDispatchedAt(), i.getResolvedAt()).toMinutes())
                .filter(m -> m >= 0).average();

        // SLA compliance
        long totalSla = all.stream().filter(i -> i.getSlaDeadline() != null).count();
        long breached  = all.stream().filter(i -> i.getSlaBreachedAt() != null).count();
        double slaCompliancePct = totalSla > 0 ? (double)(totalSla - breached) / totalSla * 100 : 100.0;

        // Per-agency performance
        List<Map<String, Object>> agencyPerf = agencyRepo.findAll().stream()
                .map(agency -> {
                    List<com.smartincident.incidentbackend.incident.entity.IncidentReport> aAll = all.stream()
                            .filter(i -> i.getLeadAgency() != null && agency.getUid().equals(i.getLeadAgency().getUid()))
                            .toList();
                    if (aAll.isEmpty()) return null;

                    OptionalDouble aAvgDispatch = aAll.stream()
                            .filter(i -> i.getReportedAt() != null && i.getDispatchedAt() != null)
                            .mapToLong(i -> java.time.Duration.between(i.getReportedAt(), i.getDispatchedAt()).toMinutes())
                            .filter(m -> m >= 0).average();

                    long aBreached = aAll.stream().filter(i -> i.getSlaBreachedAt() != null).count();
                    long aTotal    = aAll.stream().filter(i -> i.getSlaDeadline() != null).count();

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("agencyUid",  agency.getUid());
                    m.put("agencyName", agency.getName());
                    m.put("agencyType", agency.getAgencyType());
                    m.put("totalIncidents", (long) aAll.size());
                    m.put("resolved",  aAll.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED).count());
                    m.put("avgDispatchMinutes", aAvgDispatch.isPresent() ? Math.round(aAvgDispatch.getAsDouble()) : null);
                    m.put("slaBreached", aBreached);
                    m.put("slaCompliancePct", aTotal > 0 ? Math.round((double)(aTotal - aBreached) / aTotal * 1000) / 10.0 : 100.0);
                    return m;
                })
                .filter(Objects::nonNull)
                .toList();

        // Daily trend
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : incidentRepo.countGroupedByDay(since, null)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date",  row[0].toString());
            point.put("count", ((Number) row[1]).longValue());
            trend.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("totalIncidents", (long) all.size());
        result.put("avgDispatchMinutes",   avgDispatch.isPresent()   ? Math.round(avgDispatch.getAsDouble())   : null);
        result.put("avgAtSceneMinutes",    avgAtScene.isPresent()    ? Math.round(avgAtScene.getAsDouble())    : null);
        result.put("avgResolutionMinutes", avgResolution.isPresent() ? Math.round(avgResolution.getAsDouble()) : null);
        result.put("slaCompliancePct", Math.round(slaCompliancePct * 10) / 10.0);
        result.put("slaBreached", breached);
        result.put("agencyPerformance", agencyPerf);
        result.put("dailyTrend", trend);
        return Response.success(result);
    }

    /** ROOT sees all; everyone else is scoped to their agency. */
    private static String resolveAgencyScope() {
        if (LoggedUser.isRoot()) return null;
        return LoggedUser.getAgencyUid();
    }
}
