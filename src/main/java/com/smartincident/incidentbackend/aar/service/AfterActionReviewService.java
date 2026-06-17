package com.smartincident.incidentbackend.aar.service;

import com.smartincident.incidentbackend.aar.dto.AarDto;
import com.smartincident.incidentbackend.aar.entity.AfterActionReview;
import com.smartincident.incidentbackend.aar.repository.AfterActionReviewRepository;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AfterActionReviewService {

    private final AfterActionReviewRepository repository;
    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;

    public Response<AfterActionReview> submit(String incidentUid, AarDto dto) {
        IncidentReport incident = incidentRepository.findByUid(incidentUid)
                .orElse(null);
        if (incident == null) return Response.error("Incident not found");

        if (incident.getStatus() != IncidentStatus.CLOSED && incident.getStatus() != IncidentStatus.RESOLVED)
            return Response.error("AAR can only be submitted for RESOLVED or CLOSED incidents");

        AfterActionReview aar = repository.findByIncidentUid(incidentUid)
                .orElse(new AfterActionReview());

        aar.setIncident(incident);
        aar.setSummary(dto.getSummary());
        aar.setWhatWorked(dto.getWhatWorked());
        aar.setAreasForImprovement(dto.getAreasForImprovement());
        aar.setRecommendedImprovements(dto.getRecommendedImprovements());
        aar.setConductedAt(LocalDateTime.now());
        aar.setSlaBreached(incident.getSlaBreachedAt() != null);

        userRepository.findByUid(LoggedUser.getUid()).ifPresent(aar::setConductedBy);

        // Auto-compute timing metrics
        if (incident.getReportedAt() != null && incident.getDispatchedAt() != null)
            aar.setResponseTimeMinutes((int) ChronoUnit.MINUTES.between(
                    incident.getReportedAt(), incident.getDispatchedAt()));

        if (incident.getDispatchedAt() != null && incident.getAtSceneAt() != null)
            aar.setAtSceneTimeMinutes((int) ChronoUnit.MINUTES.between(
                    incident.getDispatchedAt(), incident.getAtSceneAt()));

        if (incident.getReportedAt() != null && incident.getResolvedAt() != null)
            aar.setResolutionTimeMinutes((int) ChronoUnit.MINUTES.between(
                    incident.getReportedAt(), incident.getResolvedAt()));

        AfterActionReview saved = repository.save(aar);

        // Mark incident AAR completed
        incident.setAarCompleted(true);
        incidentRepository.save(incident);

        return Response.success(saved);
    }

    public Response<AfterActionReview> get(String incidentUid) {
        return repository.findByIncidentUid(incidentUid)
                .map(Response::success)
                .orElse(Response.error("No AAR found for this incident"));
    }

    public Response<AfterActionReview> approve(String incidentUid) {
        AfterActionReview aar = repository.findByIncidentUid(incidentUid).orElse(null);
        if (aar == null) return Response.error("No AAR found for this incident");

        aar.setIsApproved(true);
        aar.setApprovedAt(LocalDateTime.now());
        userRepository.findByUid(LoggedUser.getUid()).ifPresent(aar::setApprovedBy);

        return Response.success(repository.save(aar));
    }
}
