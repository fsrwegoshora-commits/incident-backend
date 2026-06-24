package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.CorrelationType;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.incident.entity.IncidentCorrelation;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentCorrelationRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.incident.service.IncidentCorrelationService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentCorrelationController {

    private final IncidentCorrelationRepository correlationRepository;
    private final IncidentReportRepository      incidentRepository;
    private final IncidentCorrelationService    correlationService;

    /**
     * GET /api/incidents/{uid}/correlations
     * Returns all DUPLICATE and RELATED correlations for an incident (either side).
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}/correlations")
    public ResponseList<IncidentCorrelation> getCorrelations(@PathVariable String uid) {
        List<IncidentCorrelation> list = correlationRepository.findAllByIncidentUid(uid);
        return new ResponseList<>(list);
    }

    /**
     * GET /api/incidents/{uid}/co-reporters
     * Returns a summary list of users who filed duplicate reports for the same incident.
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}/co-reporters")
    public Response<Map<String, Object>> getCoReporters(@PathVariable String uid) {
        return incidentRepository.findByUid(uid)
                .map(incident -> {
                    long dupeCount = correlationRepository.countDuplicatesByMaster(uid);
                    List<Map<String, String>> reporters = (incident.getCoReporters() == null
                            ? List.<com.smartincident.incidentbackend.authotp.entity.User>of()
                            : incident.getCoReporters())
                            .stream()
                            .map(u -> Map.of(
                                "uid",   u.getUid(),
                                "name",  u.getName() != null ? u.getName() : "",
                                "phone", u.getPhoneNumber() != null ? u.getPhoneNumber() : ""))
                            .toList();
                    return Response.success(Map.<String, Object>of(
                            "incidentUid",          uid,
                            "correlatedReportCount", dupeCount,
                            "coReporters",           reporters));
                })
                .orElseGet(() -> Response.error("Incident not found"));
    }

    /**
     * POST /api/incidents/{masterUid}/correlations/link/{linkedUid}
     * Dispatcher manually links two incidents.
     *
     * Body (optional JSON):
     * {
     *   "type": "DUPLICATE" | "RELATED",
     *   "note": "optional dispatcher note"
     * }
     */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_DISPATCH_QUEUE)
    @PostMapping("/{masterUid}/correlations/link/{linkedUid}")
    public Response<IncidentCorrelation> manualLink(
            @PathVariable String masterUid,
            @PathVariable String linkedUid,
            @RequestBody(required = false) Map<String, String> body) {

        IncidentReport master = incidentRepository.findByUid(masterUid).orElse(null);
        IncidentReport linked = incidentRepository.findByUid(linkedUid).orElse(null);
        if (master == null) return Response.error("Master incident not found");
        if (linked == null) return Response.error("Linked incident not found");
        if (masterUid.equals(linkedUid)) return Response.error("Cannot link an incident to itself");

        CorrelationType type = CorrelationType.RELATED;
        String note = null;
        if (body != null) {
            if ("DUPLICATE".equalsIgnoreCase(body.get("type"))) type = CorrelationType.DUPLICATE;
            note = body.get("note");
        }

        return correlationService.manualLink(master, linked, type, note);
    }

    /**
     * GET /api/incidents/{uid}/correlations/count
     * Quick summary for dispatcher dashboard badges.
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}/correlations/count")
    public Response<Map<String, Long>> getCorrelationCount(@PathVariable String uid) {
        long duplicates = correlationRepository.countDuplicatesByMaster(uid);
        long related    = correlationRepository.findByMasterUidAndType(uid, CorrelationType.RELATED).size();
        return Response.success(Map.of("duplicates", duplicates, "related", related));
    }
}
