package com.smartincident.incidentbackend.setting.service;

import com.smartincident.incidentbackend.setting.dto.AgencyDto;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyService {
    private final AgencyRepository agencyRepository;
    public Response<Agency> saveAgency(AgencyDto agencyDto) {

        if (agencyDto == null)
            return Response.error("Agency data is required");

        log.info("User {} attempting to save agency data", LoggedUser.getName());

        // Validate required fields
        if (agencyDto.getCode() == null || agencyDto.getCode().trim().isEmpty())
            return Response.error("Agency code is required");

        if (agencyDto.getName() == null || agencyDto.getName().trim().isEmpty())
            return Response.error("Agency name is required");

        // Description is optional – remove strict validation
        String description = agencyDto.getDescription() != null ? agencyDto.getDescription().trim() : null;

        Agency agency;
        // Update mode
        if (agencyDto.getUid() != null) {
            Optional<Agency> existingAgency = agencyRepository.findByUid(agencyDto.getUid());
            if (existingAgency.isEmpty())
                return Response.error("Agency not found with UID: " + agencyDto.getUid());

            agency = existingAgency.get();
        } else {
            // Create mode
            if (agencyRepository.existsByCode(agencyDto.getCode().trim())) {
                return Response.error("Agency code already exists");
            }
            agency = new Agency();
        }

        // Set fields
        agency.setCode(agencyDto.getCode().trim());
        agency.setName(agencyDto.getName().trim());
        agency.setDescription(description);
        agency.update();

        try {
            Agency savedAgency = agencyRepository.save(agency);
            log.info("Agency with UID {} saved successfully", savedAgency.getUid());
            return Response.success(savedAgency);
        } catch (Exception e) {
            log.error("Error saving agency", e);
            return Response.error("Failed to save agency");
        }
    }

    public Response<Agency> deleteAgency(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<Agency> agencyOpt = agencyRepository.findByUid(uid);
        if (agencyOpt.isEmpty())
            return new Response<>("Invalid agency provided");
        if (!agencyOpt.get().getIsActive())
            return new Response<>("agency already deleted");
        agencyOpt.get().delete();
        Agency agency = agencyOpt.get();
        try {
            agencyRepository.save(agency);
            log.info("agency deleted successfully: {}", agency.getName());
        } catch (Exception e) {
            log.error("Failed to delete agency: {}", e.getMessage());
            String message = Utils.getExceptionMessage(e);
            return new Response<>(message);
        }
        return Response.success(agency);
    }

    public Response<Agency> getAgencyByUid(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<Agency> agencyOpt = agencyRepository.findByUid(uid);
        if (agencyOpt.isEmpty())
            return new Response<>("Invalid agency provided");
        return Response.success(agencyOpt.get());
    }

    public ResponsePage<Agency> getAgencies(PageableParam pageableParam) {
        return new ResponsePage<>(agencyRepository.gatAgencies(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key()));
    }

}
