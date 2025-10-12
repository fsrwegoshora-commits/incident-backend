package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.police.dto.PoliceOfficerDto;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class PoliceOfficerService {

    private final PoliceOfficerRepository policeOfficerRepository;
    private final PoliceStationRepository policeStationRepository;
    private final UserRepository userRepository;

    public Response<PoliceOfficer> savePoliceOfficer(PoliceOfficerDto dto) {
        if (dto == null) return Response.error("Police officer DTO cannot be null");

        PoliceOfficer officer = new PoliceOfficer();
        if (dto.getUid() != null) {
            Optional<PoliceOfficer> existing = policeOfficerRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Invalid police officer provided");

            officer = existing.get();
            Utils.copyProperties(dto, officer);
            officer.setCode(dto.getCode());

            if (dto.getStationUid() != null) {
                Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getStationUid());
                if (stationOpt.isEmpty()) return Response.error("Station not found");
                officer.setStation(stationOpt.get());
            }

            if (dto.getUserUid() != null) {
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                officer.setUserAccount(userOpt.get());
            }
            officer.update();
        }

        // CREATE FLOW
        else {
            if (dto.getBadgeNumber() == null) return Response.error("Badge number is required");
            if (dto.getCode() == null) return Response.error("Officer rank is required");
            if (dto.getStationUid() == null) return Response.error("Station is required");
            if (dto.getUserUid() == null) return Response.error("User is required");

            // Check if user already assigned
            Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
            if (userOpt.isEmpty()) return Response.error("User not found");

            boolean alreadyAssigned = policeOfficerRepository.existsByUserAccount(userOpt.get());
            if (alreadyAssigned) return Response.error("This user is already assigned as a police officer");

            Utils.copyProperties(dto, officer);
            officer.setCode(dto.getCode());
            officer.setUserAccount(userOpt.get());

            Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getStationUid());
            if (stationOpt.isEmpty()) return Response.error("Station not found");
            officer.setStation(stationOpt.get());
        }

        try {
            PoliceOfficer saved = policeOfficerRepository.save(officer);
            log.info("Successfully saved police officer with badge: {}", saved.getBadgeNumber());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save police officer: {}", e.getMessage());
            return Response.error("Failed to save police officer: " + Utils.getExceptionMessage(e));
        }
    }


    public Response<PoliceOfficer> getPoliceOfficer(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<PoliceOfficer> optionalPoliceOfficer = policeOfficerRepository.findByUid(uid);
        return optionalPoliceOfficer.map(Response::new).orElseGet(() -> new Response<>("Invalid police police officer provided"));
    }

    public Response<PoliceOfficer> deletePoliceOfficer(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<PoliceOfficer> oPoliceOfficer = policeOfficerRepository.findByUid(uid);
        if (oPoliceOfficer.isEmpty())
            return new Response<>("Invalid police officer provided");
        if (!oPoliceOfficer.get().getIsActive())
            return new Response<>("police officer already deleted");
        oPoliceOfficer.get().delete();
        PoliceOfficer policeOfficer = oPoliceOfficer.get();
        try {
            policeOfficerRepository.save(policeOfficer);
            log.info("police officer deleted successfully: {}", policeOfficer.getUserAccount().getName());
        } catch (Exception e) {
            log.error("Failed to delete police officer: {}", e.getMessage());
            String message = Utils.getExceptionMessage(e);
            return new Response<>(message);
        }
        return Response.success(policeOfficer);
    }

    public ResponsePage<PoliceOfficer> getPoliceOfficers(PageableParam pageableParam) {
        String stationUid = LoggedUser.getStationUid();
        return new ResponsePage<>(policeOfficerRepository.getPoliceOfficers(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(),stationUid));
    }

    public ResponsePage<PoliceOfficer> getPoliceOfficersByStation(PageableParam pageableParam, String stationUid) {
        return new ResponsePage<>(policeOfficerRepository.getPoliceOfficersByStation(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), stationUid));
    }
}
