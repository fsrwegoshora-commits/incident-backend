package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.aar.dto.AarDto;
import com.smartincident.incidentbackend.aar.entity.AfterActionReview;
import com.smartincident.incidentbackend.aar.service.AfterActionReviewService;
import com.smartincident.incidentbackend.incident.dto.CommandStructureDto;
import com.smartincident.incidentbackend.incident.dto.DesignateCommanderRequest;
import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.dto.IncidentTimelineDto;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.entity.IncidentStatusHistory;
import com.smartincident.incidentbackend.incident.service.IncidentReportService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentReportController {

    private final IncidentReportService incidentService;
    private final AfterActionReviewService aarService;

    @Authenticated
    @RequiresPermission(Permission.CREATE_INCIDENT)
    @PostMapping
    public Response<IncidentReport> createIncident(@RequestBody IncidentReportDto dto) {
        log.info("Creating incident: {}", dto.getTitle());
        return incidentService.createIncident(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.UPDATE_INCIDENT)
    @PutMapping
    public Response<IncidentReport> updateIncident(@RequestBody IncidentReportDto dto) {
        log.info("Updating incident: {}", dto.getUid());
        return incidentService.updateIncident(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.ASSIGN_OFFICER)
    @PutMapping("/{incidentUid}/assign-officer/{officerUid}")
    public Response<IncidentReport> assignOfficer(@PathVariable String incidentUid,
                                                  @PathVariable String officerUid) {
        log.info("Assigning officer to incident");
        return incidentService.assignOfficer(incidentUid, officerUid);
    }

    @Authenticated
    @RequiresPermission(Permission.CLOSE_INCIDENT)
    @DeleteMapping("/{uid}")
    public Response<IncidentReport> deleteIncident(@PathVariable String uid) {
        log.info("Closing/deleting incident: {}", uid);
        return incidentService.deleteIncident(uid);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<IncidentReport> getIncident(@PathVariable String uid) {
        return incidentService.getIncident(uid);
    }

    @Authenticated
    @GetMapping("/my")
    public ResponsePage<IncidentReport> getMyIncidents(@ModelAttribute PageableParam pageableParam) {
        log.info("Getting my incidents");
        return incidentService.getMyIncidents(pageableParam);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_PENDING_INCIDENTS)
    @GetMapping("/pending")
    public ResponsePage<IncidentReport> getPendingStationIncidents(
            @ModelAttribute PageableParam pageableParam) {
        log.info("Getting pending incidents for dispatcher");
        return incidentService.getPendingStationIncidents(pageableParam);
    }

    /** Advance an incident through the standardised lifecycle state machine. */
    @Authenticated
    @RequiresPermission(Permission.UPDATE_INCIDENT)
    @PutMapping("/{uid}/status")
    public Response<IncidentReport> transitionStatus(
            @PathVariable String uid,
            @RequestParam IncidentStatus status,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        log.info("Transitioning incident {} to {}", uid, status);
        return incidentService.transitionStatus(uid, status, note);
    }

    /** Full audit trail of status transitions for an incident. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}/history")
    public Response<List<IncidentStatusHistory>> getStatusHistory(@PathVariable String uid) {
        return incidentService.getStatusHistory(uid);
    }

    /** Dispatch queue — scoped by role (DISPATCHER: own queue, DC_ADMIN/SUPERVISOR: entire center, ROOT: all). */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_DISPATCH_QUEUE)
    @GetMapping("/dispatch-queue")
    public ResponsePage<IncidentReport> getDispatchQueue(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) IncidentStatus status) {
        log.info("Getting dispatch queue for dispatcher");
        return incidentService.getDispatchQueue(pageableParam, status);
    }

    @Authenticated
    @RequiresPermission(Permission.UPDATE_INCIDENT)
    @GetMapping("/station")
    public ResponsePage<IncidentReport> getStationIncidents(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) IncidentStatus status) {
        log.info("Getting station incidents");
        return incidentService.getStationIncidents(pageableParam, status);
    }

    /** Investigation queue — NON_EMERGENCY cases assigned to the station, bypassing dispatch. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_INVESTIGATION_QUEUE)
    @GetMapping("/investigation-queue")
    public ResponsePage<IncidentReport> getInvestigationQueue(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) IncidentStatus status) {
        log.info("Getting investigation queue");
        return incidentService.getInvestigationQueue(pageableParam, status);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_OFFICER_INCIDENTS)
    @GetMapping("/officer")
    public ResponsePage<IncidentReport> getOfficerIncidents(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) IncidentStatus status) {
        log.info("Getting officer incidents");
        return incidentService.getOfficerIncidents(pageableParam, status);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_OFFICER_INCIDENTS)
    @GetMapping("/nearby")
    public Response<List<IncidentReport>> getNearbyIncidents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm,
            @RequestParam(required = false) IncidentStatus status) {
        log.info("Getting nearby incidents");
        return incidentService.getNearbyIncidents(latitude, longitude, radiusKm, status);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_ANALYTICS)
    @GetMapping("/stats")
    public Response<IncidentReportService.IncidentStats> getIncidentStats(
            @RequestParam(required = false) String stationUid) {
        return incidentService.getStationStats(stationUid);
    }

    /** Dispatcher escalates incident to supervisor / DC admin when resources are unavailable. */
    @Authenticated
    @RequiresPermission(Permission.ESCALATE_INCIDENT)
    @PostMapping("/{uid}/escalate")
    public Response<IncidentReport> escalateIncident(
            @PathVariable String uid,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        log.info("Escalating incident {}", uid);
        return incidentService.escalateIncident(uid, note);
    }

    /** Return all incidents that have breached their SLA deadline. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_SLA_BREACHES)
    @GetMapping("/sla-breached")
    public ResponsePage<IncidentReport> getSlaBreachedIncidents(@ModelAttribute PageableParam pageableParam) {
        return incidentService.getSlaBreachedIncidents(pageableParam);
    }

    /** Return incidents approaching SLA deadline within the next N minutes. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_SLA_METRICS)
    @GetMapping("/sla-approaching")
    public ResponsePage<IncidentReport> getSlaApproachingIncidents(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(defaultValue = "10") int withinMinutes) {
        return incidentService.getSlaApproachingIncidents(pageableParam, withinMinutes);
    }

    // ── Incident Commander (Critical 4) ───────────────────────────────────────

    /** Designate an incident commander and optionally set the lead agency. */
    @Authenticated
    @RequiresPermission(Permission.DESIGNATE_COMMANDER)
    @PostMapping("/{uid}/commander")
    public Response<IncidentReport> designateCommander(
            @PathVariable String uid,
            @RequestBody DesignateCommanderRequest req) {
        log.info("Designating commander for incident {}", uid);
        return incidentService.designateCommander(uid, req);
    }

    /** Get the full command structure — commander, lead agency, support agencies. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}/command-structure")
    public Response<CommandStructureDto> getCommandStructure(@PathVariable String uid) {
        return incidentService.getCommandStructure(uid);
    }

    // ── Citizen Timeline (High 1) ─────────────────────────────────────────────

    /** Rich incident timeline — status history, responders, timestamps, ETA. Open to the incident reporter. */
    @Authenticated
    @GetMapping("/{uid}/timeline")
    public Response<IncidentTimelineDto> getIncidentTimeline(@PathVariable String uid) {
        return incidentService.getIncidentTimeline(uid);
    }

    // ── Incident Closure Review (V2) ─────────────────────────────────────────

    /** Station Admin approves the resolved incident before it can be closed by Dispatcher. */
    @Authenticated
    @RequiresPermission(Permission.STATION_REVIEW_INCIDENT)
    @PostMapping("/{uid}/station-review")
    public Response<IncidentReport> stationReview(
            @PathVariable String uid,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("notes", "") : "";
        log.info("Station review for incident {}", uid);
        return incidentService.stationReview(uid, notes);
    }

    /** Dispatcher marks final sign-off after station review — incident can then be CLOSED. */
    @Authenticated
    @RequiresPermission(Permission.DISPATCHER_REVIEW_INCIDENT)
    @PostMapping("/{uid}/dispatcher-review")
    public Response<IncidentReport> dispatcherReview(
            @PathVariable String uid,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("notes", "") : "";
        log.info("Dispatcher review for incident {}", uid);
        return incidentService.dispatcherReview(uid, notes);
    }

    // ── After Action Review (V2) ─────────────────────────────────────────────

    /** Submit an After Action Review for a closed/resolved incident. */
    @Authenticated
    @RequiresPermission(Permission.SUBMIT_AAR)
    @PostMapping("/{uid}/aar")
    public Response<AfterActionReview> submitAar(
            @PathVariable String uid,
            @RequestBody AarDto dto) {
        log.info("Submitting AAR for incident {}", uid);
        return aarService.submit(uid, dto);
    }

    /** Retrieve the After Action Review for an incident. */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_AFTER_ACTION)
    @GetMapping("/{uid}/aar")
    public Response<AfterActionReview> getAar(@PathVariable String uid) {
        return aarService.get(uid);
    }

    /** Agency Admin or ROOT approves a submitted AAR. */
    @Authenticated
    @RequiresPermission(Permission.APPROVE_AAR)
    @PostMapping("/{uid}/aar/approve")
    public Response<AfterActionReview> approveAar(@PathVariable String uid) {
        log.info("Approving AAR for incident {}", uid);
        return aarService.approve(uid);
    }
}
