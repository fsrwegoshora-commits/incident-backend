package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.DepartmentType;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.police.dto.TrafficCheckPointDto;
import com.smartincident.incidentbackend.police.entity.*;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.TrafficCheckPointRepository;
import com.smartincident.incidentbackend.setting.entity.Department;
import com.smartincident.incidentbackend.setting.repository.DepartmentRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrafficCheckPointService {
    private final PoliceOfficerRepository policeOfficerRepository;
    private final TrafficCheckPointRepository trafficCheckPointRepository;
    private final PoliceStationRepository policeStationRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationService notificationService;
    public Response<TrafficCheckpoint> saveTrafficCheckpoint(TrafficCheckPointDto dto) {

        if (dto == null)
            return Response.error("Checkpoint data is required");

        log.info("User {} attempting to save checkpoint data", LoggedUser.getName());

        // Required fields
        if (dto.getName() == null || dto.getName().trim().isEmpty())
            return Response.error("Checkpoint name is required");

        if (dto.getLocation() == null)
            return Response.error("Location is required");

        if (dto.getPoliceStationUid() == null)
            return Response.error("Police station UID is required");

        if (dto.getDepartmentUid() == null)
            return Response.error("Department UID is required");

        // Lookup station
        Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getPoliceStationUid());
        if (stationOpt.isEmpty())
            return Response.error("Invalid police station UID");

        // Lookup department
        Optional<Department> deptOpt = departmentRepository.findByUid(dto.getDepartmentUid());
        if (deptOpt.isEmpty())
            return Response.error("Invalid department UID");

        Department department = deptOpt.get();

        // Domain rule: checkpoint must belong to TRAFFIC department
        if (department.getType() != DepartmentType.TRAFFIC_POLICE)
            return Response.error("Checkpoint must belong to TRAFFIC department");

        TrafficCheckpoint checkpoint;

        // Update mode
        if (dto.getUid() != null) {
            Optional<TrafficCheckpoint> existing = trafficCheckPointRepository.findByUid(dto.getUid());
            if (existing.isEmpty())
                return Response.error("Checkpoint not found with UID: " + dto.getUid());

            checkpoint = existing.get();
        } else {
            checkpoint = new TrafficCheckpoint();
        }

        // Set fields
        checkpoint.setName(dto.getName().trim());
        checkpoint.setContactPhone(dto.getContactInfo());
        checkpoint.setCoverageRadiusKm(dto.getCoverageRadiusKm());
        checkpoint.setParentStation(stationOpt.get());
        checkpoint.setDepartment(department);
        checkpoint.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Map location
        checkpoint.setLocation(new Location(
                dto.getLocation().getLatitude(),
                dto.getLocation().getLongitude(),
                dto.getLocation().getAddress()
        ));

        checkpoint.update();

        try {
            TrafficCheckpoint saved = trafficCheckPointRepository.save(checkpoint);
            log.info("Checkpoint {} saved successfully", saved.getName());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save checkpoint", e);
            return Response.error("Failed to save checkpoint");
        }
    }

    public Response<TrafficCheckpoint> deleteTrafficCheckpoint(String uid) {

        if (uid == null)
            return Response.error("UID is required");

        Optional<TrafficCheckpoint> cpOpt = trafficCheckPointRepository.findByUid(uid);
        if (cpOpt.isEmpty())
            return Response.error("Invalid checkpoint UID");

        TrafficCheckpoint cp = cpOpt.get();

        if (!cp.getIsActive())
            return Response.error("Checkpoint already deleted");

        cp.delete();

        try {
            trafficCheckPointRepository.save(cp);
            log.info("Checkpoint {} deleted successfully", cp.getName());
            return Response.success(cp);
        } catch (Exception e) {
            log.error("Failed to delete checkpoint", e);
            return Response.error("Failed to delete checkpoint");
        }
    }

    public Response<TrafficCheckpoint> getTrafficCheckpointByUid(String uid) {
        if (uid == null)
            return Response.error("UID is required");

        Optional<TrafficCheckpoint> cpOpt = trafficCheckPointRepository.findByUid(uid);
        if (cpOpt.isEmpty())
            return Response.error("Invalid checkpoint UID");

        return Response.success(cpOpt.get());
    }

    public ResponsePage<TrafficCheckpoint> getCheckpoints(PageableParam pageableParam) {
        return new ResponsePage<>(
                trafficCheckPointRepository.getCheckpoints(
                        pageableParam.getPageable(true),
                        pageableParam.getIsActive(),
                        pageableParam.key()
                )
        );
    }

    public Response<TrafficCheckpoint> assignSupervisor(String checkpointUid, String officerUid) {

        Optional<TrafficCheckpoint> cpOpt = trafficCheckPointRepository.findByUid(checkpointUid);
        if (cpOpt.isEmpty()) return Response.error("Checkpoint not found");

        Optional<PoliceOfficer> officerOpt = policeOfficerRepository.findByUid(officerUid);
        if (officerOpt.isEmpty()) return Response.error("Officer not found");

        PoliceOfficer officer = officerOpt.get();

        if (!officer.getDepartment().getType().equals(DepartmentType.TRAFFIC_POLICE)) {
            return Response.error("Supervisor must be a traffic officer");
        }

        TrafficCheckpoint cp = cpOpt.get();
        cp.setSupervisingOfficer(officer);
        cp.update();

        TrafficCheckpoint saved = trafficCheckPointRepository.save(cp);

        notifySupervisorAssigned(saved);

        return Response.success(saved);
    }

    public Response<TrafficCheckpoint> changeSupervisor(String checkpointUid, String newOfficerUid) {

        Optional<TrafficCheckpoint> cpOpt = trafficCheckPointRepository.findByUid(checkpointUid);
        if (cpOpt.isEmpty()) return Response.error("Checkpoint not found");

        Optional<PoliceOfficer> officerOpt = policeOfficerRepository.findByUid(newOfficerUid);
        if (officerOpt.isEmpty()) return Response.error("New supervisor not found");

        PoliceOfficer newOfficer = officerOpt.get();

        if (!newOfficer.getDepartment().getType().equals(DepartmentType.TRAFFIC_POLICE)) {
            return Response.error("Supervisor must be a traffic officer");
        }

        TrafficCheckpoint cp = cpOpt.get();
        PoliceOfficer oldSupervisor = cp.getSupervisingOfficer();

        cp.setSupervisingOfficer(newOfficer);
        cp.update();

        TrafficCheckpoint saved = trafficCheckPointRepository.save(cp);

        notifyOldSupervisorRemoved(oldSupervisor, saved);
        notifySupervisorChanged(saved);
        return Response.success(saved);
    }

    private void notifySupervisorChanged(TrafficCheckpoint checkpoint) {

        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setTitle("Checkpoint Supervisor Assignment");
        notificationDto.setMessage(
                "You have been assigned as the supervisor for checkpoint: "
                        + checkpoint.getName()
        );
        notificationDto.setType(NotificationType.SUPERVISOR_ALERT);
        notificationDto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        notificationDto.setRelatedEntityUid(checkpoint.getUid());
        notificationDto.setRelatedEntityType("CHECKPOINT");
        notificationDto.setTargetUserUids(
                List.of(checkpoint.getSupervisingOfficer().getUserAccount().getUid())
        );

        notificationService.sendNotification(notificationDto);
    }


    private void notifyOldSupervisorRemoved(PoliceOfficer oldSupervisor, TrafficCheckpoint checkpoint) {
        if (oldSupervisor == null) return;

        NotificationDto dto = new NotificationDto();
        dto.setTitle("Checkpoint Supervisor Change");
        dto.setMessage(
                "You have been removed as the supervisor for checkpoint: "
                        + checkpoint.getName()
        );
        dto.setType(NotificationType.SUPERVISOR_ALERT);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(checkpoint.getUid());
        dto.setRelatedEntityType("CHECKPOINT");
        dto.setTargetUserUids(List.of(oldSupervisor.getUserAccount().getUid()));

        notificationService.sendNotification(dto);
    }

    private void notifySupervisorAssigned(TrafficCheckpoint checkpoint) {

        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setTitle("Checkpoint Supervisor Assignment");
        notificationDto.setMessage(
                "You have been assigned as the supervisor for checkpoint: "
                        + checkpoint.getName()
        );
        notificationDto.setType(NotificationType.SUPERVISOR_ALERT);
        notificationDto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        notificationDto.setRelatedEntityUid(checkpoint.getUid());
        notificationDto.setRelatedEntityType("CHECKPOINT");
        notificationDto.setTargetUserUids(
                List.of(checkpoint.getSupervisingOfficer().getUserAccount().getUid())
        );

        notificationService.sendNotification(notificationDto);
    }

    public ResponsePage<TrafficCheckpoint> getTrafficCheckpointsByPoliceStation(PageableParam pageableParam, String stationUid) {
        return new ResponsePage<>(
                trafficCheckPointRepository.getTrafficCheckpointsByPoliceStation(
                        pageableParam.getPageable(true),
                        pageableParam.getIsActive(),
                        pageableParam.key(),
                        stationUid
                )
        );
    }
}
