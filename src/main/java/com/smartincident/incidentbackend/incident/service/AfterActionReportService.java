package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.incident.dto.AfterActionReportDto;
import com.smartincident.incidentbackend.incident.entity.AfterActionReport;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.AfterActionReportRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AfterActionReportService {

    private final AfterActionReportRepository aarRepository;
    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Response<AfterActionReport> createOrUpdate(AfterActionReportDto dto) {
        if (dto.getIncidentUid() == null)   return Response.error("Incident UID is required");
        if (dto.getExecutiveSummary() == null || dto.getExecutiveSummary().isBlank())
            return Response.error("Executive summary is required");

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(dto.getIncidentUid());
        if (incidentOpt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = incidentOpt.get();

        if (incident.getStatus() != IncidentStatus.RESOLVED && incident.getStatus() != IncidentStatus.CLOSED)
            return Response.error("After-action reports can only be filed for RESOLVED or CLOSED incidents");

        String authorUid = LoggedUser.getUid();
        User author = authorUid != null ? userRepository.findByUid(authorUid).orElse(null) : null;
        if (author == null) return Response.error("Author not found");

        AfterActionReport aar;
        Optional<AfterActionReport> existing = dto.getUid() != null
                ? aarRepository.findByUid(dto.getUid())
                : aarRepository.findByIncidentUid(dto.getIncidentUid());

        if (existing.isPresent()) {
            aar = existing.get();
            if (Boolean.TRUE.equals(aar.getIsApproved()))
                return Response.error("Approved reports cannot be modified");
        } else {
            aar = new AfterActionReport();
            aar.setIncident(incident);
            aar.setAuthoredBy(author);
        }

        aar.setExecutiveSummary(dto.getExecutiveSummary());
        aar.setTimeline(dto.getTimeline());
        aar.setWhatWentWell(dto.getWhatWentWell());
        aar.setWhatWentWrong(dto.getWhatWentWrong());
        aar.setRootCauseAnalysis(dto.getRootCauseAnalysis());
        aar.setRecommendedActions(dto.getRecommendedActions());
        aar.setLessonsLearned(dto.getLessonsLearned());
        aar.setAgencyCoordinationNotes(dto.getAgencyCoordinationNotes());
        aar.setSlaBreached(incident.getSlaBreachedAt() != null);

        // Compute timing metrics from incident timestamps
        if (incident.getReportedAt() != null && incident.getDispatchedAt() != null)
            aar.setResponseTimeMinutes(Duration.between(incident.getReportedAt(), incident.getDispatchedAt()).toMinutes());
        if (incident.getDispatchedAt() != null && incident.getAtSceneAt() != null)
            aar.setDispatchToArrivalMinutes(Duration.between(incident.getDispatchedAt(), incident.getAtSceneAt()).toMinutes());
        if (incident.getReportedAt() != null && incident.getResolvedAt() != null)
            aar.setTotalResolutionMinutes(Duration.between(incident.getReportedAt(), incident.getResolvedAt()).toMinutes());

        aar.update();

        try {
            aar = aarRepository.save(aar);
            log.info("After-action report saved for incident: {}", dto.getIncidentUid());
            return Response.success(aar);
        } catch (Exception e) {
            log.error("Failed to save after-action report: {}", e.getMessage());
            return Response.error("Failed to save report: " + Utils.getExceptionMessage(e));
        }
    }

    @Transactional
    public Response<AfterActionReport> approve(String aarUid) {
        Optional<AfterActionReport> opt = aarRepository.findByUid(aarUid);
        if (opt.isEmpty()) return Response.error("After-action report not found");
        AfterActionReport aar = opt.get();
        if (Boolean.TRUE.equals(aar.getIsApproved()))
            return Response.error("Report is already approved");

        String approverUid = LoggedUser.getUid();
        User approver = approverUid != null ? userRepository.findByUid(approverUid).orElse(null) : null;

        aar.setIsApproved(true);
        aar.setApprovedBy(approver);
        aar.update();

        try {
            aar = aarRepository.save(aar);
            log.info("After-action report approved: {}", aarUid);
            return Response.success(aar);
        } catch (Exception e) {
            return Response.error("Failed to approve report: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<AfterActionReport> getByIncident(String incidentUid) {
        return aarRepository.findByIncidentUid(incidentUid)
                .map(Response::success)
                .orElseGet(() -> Response.error("No after-action report found for this incident"));
    }

    public Response<AfterActionReport> getByUid(String uid) {
        return aarRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("After-action report not found"));
    }

    public ResponsePage<AfterActionReport> getAll(PageableParam pageableParam, Boolean approved, String authorUid) {
        return new ResponsePage<>(aarRepository.findFiltered(approved, authorUid, pageableParam.getPageable(true)));
    }
}
