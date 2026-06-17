package com.smartincident.incidentbackend.setting.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.OfficerRank;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.setting.dto.AgencyDto;
import com.smartincident.incidentbackend.setting.dto.OfficerSummaryDto;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyService {
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final PoliceOfficerRepository policeOfficerRepository;

    public Response<Agency> saveAgency(AgencyDto agencyDto) {

        if (agencyDto == null)
            return Response.error("Agency data is required");

        log.info("User {} attempting to save agency data", LoggedUser.getName());

        // Validate required fields
        String effectiveCode = agencyDto.getEffectiveCode();
        if (effectiveCode == null || effectiveCode.trim().isEmpty())
            return Response.error("Agency type is required");

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
            // Create mode — allow multiple agencies of the same type; only block exact name+type duplicates
            agency = new Agency();
        }

        // Set fields
        agency.setCode(effectiveCode.trim());
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

    public Response<List<User>> getAgencyAdmins(String agencyUid) {
        if (agencyUid == null || agencyUid.isBlank())
            return Response.error("Agency UID is required");
        Optional<Agency> agencyOpt = agencyRepository.findByUid(agencyUid);
        if (agencyOpt.isEmpty())
            return Response.error("Agency not found");
        List<User> admins = userRepository.findByAgencyUidAndRoleAndIsActiveTrue(agencyUid, Role.AGENCY_ADMIN);
        log.info("Found {} AGENCY_ADMIN user(s) for agency {}", admins.size(), agencyUid);
        return Response.success(admins);
    }

    /**
     * Returns officers available to be assigned as agency admin.
     * POLICE → all active PoliceOfficer records (with rank).
     * FIRE   → all active users with FIRE_OFFICER role.
     * Other  → empty list (MEDICAL/ECA admins are created fresh, no officer pool).
     */
    public Response<List<OfficerSummaryDto>> getAvailableOfficers(String agencyUid) {
        Optional<Agency> agencyOpt = agencyRepository.findByUid(agencyUid);
        if (agencyOpt.isEmpty()) return Response.error("Agency not found");

        String agencyType = agencyOpt.get().getCode();
        List<OfficerSummaryDto> summaries = new ArrayList<>();

        if ("POLICE".equalsIgnoreCase(agencyType)) {
            List<PoliceOfficer> officers = policeOfficerRepository.findByIsActiveTrue();
            for (PoliceOfficer o : officers) {
                if (o.getUserAccount() == null) continue;
                summaries.add(new OfficerSummaryDto(
                    o.getUserAccount().getUid(),
                    o.getUserAccount().getName(),
                    o.getCode() != null ? o.getCode().name() : null,
                    o.getCode() != null ? rankLabel(o.getCode()) : null,
                    o.getBadgeNumber(),
                    o.getPoliceStation() != null ? o.getPoliceStation().getName() : null
                ));
            }
        } else if ("FIRE".equalsIgnoreCase(agencyType)) {
            List<User> fireOfficers = userRepository.findByRoleAndIsActiveTrue(Role.FIRE_OFFICER);
            for (User u : fireOfficers) {
                summaries.add(new OfficerSummaryDto(
                    u.getUid(), u.getName(), null, null, null,
                    u.getEmergencyUnit() != null ? u.getEmergencyUnit().getName() : null
                ));
            }
        }

        return Response.success(summaries);
    }

    /**
     * Promotes an existing officer/user to AGENCY_ADMIN and links them to the given agency.
     */
    public Response<User> assignOfficerAsAdmin(String agencyUid, String userUid) {
        Optional<Agency> agencyOpt = agencyRepository.findByUid(agencyUid);
        if (agencyOpt.isEmpty()) return Response.error("Agency not found");

        Optional<User> userOpt = userRepository.findByUid(userUid);
        if (userOpt.isEmpty()) return Response.error("User not found");

        User user = userOpt.get();
        if (user.getRole() == Role.ROOT)
            return Response.error("Cannot change the role of a ROOT user");
        if (user.getRole() == Role.AGENCY_ADMIN && agencyOpt.get().equals(user.getAgency()))
            return Response.error("User is already an admin for this agency");

        user.setRole(Role.AGENCY_ADMIN);
        user.setAgency(agencyOpt.get());
        user.update();

        try {
            User saved = userRepository.save(user);
            log.info("Assigned '{}' ({}) as AGENCY_ADMIN for agency '{}'",
                saved.getName(), saved.getUid(), agencyOpt.get().getName());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to assign officer as admin: {}", e.getMessage());
            return Response.error("Failed to assign officer: " + Utils.getExceptionMessage(e));
        }
    }

    private String rankLabel(OfficerRank rank) {
        return switch (rank) {
            case PC    -> "Police Constable";
            case CPL   -> "Corporal";
            case SGT   -> "Sergeant";
            case S_SGT -> "Staff Sergeant";
            case SM    -> "Sergeant Major";
            case A_ISP -> "Asst. Inspector";
            case ISP   -> "Inspector";
        };
    }

}
