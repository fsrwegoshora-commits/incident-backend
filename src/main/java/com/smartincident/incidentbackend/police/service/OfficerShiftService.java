package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.enums.ShiftDutyType;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.police.dto.BulkCheckpointShiftDto;
import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.TrafficCheckPointRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfficerShiftService {
    private final OfficerShiftRepository officerShiftRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final PoliceStationRepository policeStationRepository;
    private final NotificationService notificationService;
    private final TrafficCheckPointRepository trafficCheckpointRepository;

    public Response<OfficerShift> saveShift(OfficerShiftDto dto) {
        if (dto == null) return Response.error("Shift DTO cannot be null");

        OfficerShift shift = new OfficerShift();

        // Update mode
        if (dto.getUid() != null) {
            Optional<OfficerShift> existing = officerShiftRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Invalid shift UID");
            shift = existing.get();
        }

        // Required fields
        if (dto.getOfficerUid() == null) return Response.error("Officer UID is required");
        if (dto.getShiftDate() == null) return Response.error("Shift date is required");
        if (dto.getShiftTime() == null) return Response.error("Shift time is required");
        if (dto.getShiftDutyType() == null) return Response.error("Shift duty type is required");
        if (dto.getStartTime() == null) return Response.error("Start time is required");
        if (dto.getEndTime() == null) return Response.error("End time is required");

        // Officer lookup
        Optional<PoliceOfficer> officerOpt = policeOfficerRepository.findByUid(dto.getOfficerUid());
        if (officerOpt.isEmpty()) return Response.error("Officer not found");

        // CHECKPOINT VALIDATION
        if (dto.getShiftDutyType() == ShiftDutyType.CHECKPOINT_DUTY && dto.getCheckpointUid() == null) {
            return Response.error("Checkpoint is required for CHECKPOINT_DUTY");
        }

        // Load checkpoint if provided
        TrafficCheckpoint checkpoint = null;
        if (dto.getCheckpointUid() != null) {
            Optional<TrafficCheckpoint> checkpointOpt = trafficCheckpointRepository.findByUid(dto.getCheckpointUid());
            if (checkpointOpt.isEmpty()) return Response.error("Invalid checkpoint UID");
            checkpoint = checkpointOpt.get();
            shift.setCheckpoint(checkpoint);
        } else {
            shift.setCheckpoint(null);
        }

        if (checkpoint != null && checkpoint.getSupervisingOfficer() != null) {
            PoliceOfficer supervisor = checkpoint.getSupervisingOfficer();
            if (supervisor.getUid().equals(dto.getOfficerUid())) {
                return Response.error("Supervisor cannot be assigned as a checkpoint shift officer");
            }
        }

        // OVERLAP VALIDATION
        List<OfficerShift> existingShifts =
                officerShiftRepository.findByOfficerUidAndShiftDate(dto.getOfficerUid(), dto.getShiftDate());

        for (OfficerShift existingShift : existingShifts) {
            if (dto.getUid() == null || !existingShift.getUid().equals(dto.getUid())) {
                if (isShiftOverlap(dto.getStartTime(), dto.getEndTime(),
                        existingShift.getStartTime(), existingShift.getEndTime())) {
                    return Response.error("Officer already has a conflicting shift on this date");
                }
            }
        }

        // Set fields
        shift.setOfficer(officerOpt.get());
        shift.setShiftDate(dto.getShiftDate());
        shift.setShiftTime(dto.getShiftTime());
        shift.setShiftDutyType(dto.getShiftDutyType());
        shift.setDutyDescription(dto.getDutyDescription());
        shift.setIsPunishmentMode(dto.getIsPunishmentMode());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setIsExcused(dto.getIsExcused());
        shift.setExcuseReason(dto.getExcuseReason());
        shift.setIsReassigned(dto.getIsReassigned());

        if (dto.getUid() != null) {
            shift.update();
        }

        try {
            OfficerShift saved = officerShiftRepository.save(shift);
            notifyShiftAssignment(saved);
            return Response.success(saved);
        } catch (Exception e) {
            return Response.error("Failed to save shift: " + Utils.getExceptionMessage(e));
        }
    }


    public Response<OfficerShift> excuseShift(String shiftUid, String reason) {
        if (shiftUid == null || reason == null) {
            return Response.error("Shift UID and reason are required");
        }

        Optional<OfficerShift> shiftOpt = officerShiftRepository.findByUid(shiftUid);
        if (shiftOpt.isEmpty()) return Response.error("Shift not found");

        OfficerShift shift = shiftOpt.get();
        shift.setIsExcused(true);
        shift.setExcuseReason(reason);
        shift.update();

        try {
            OfficerShift saved = officerShiftRepository.save(shift);
            log.info("Excused shift {} for officer {}", saved.getUid(), saved.getOfficer().getBadgeNumber());
            notifyShiftExcuse(shift);

            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to excuse shift: {}", e.getMessage());
            return Response.error("Failed to excuse shift: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<OfficerShift> deleteOfficerShift(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<OfficerShift> oOfficerShift = officerShiftRepository.findByUid(uid);
        if (oOfficerShift.isEmpty())
            return new Response<>("Invalid police officer shift provided");
        if (!oOfficerShift.get().getIsActive())
            return new Response<>("police officer shift already deleted");
        oOfficerShift.get().delete();
        OfficerShift officerShift = oOfficerShift.get();
        try {
            officerShiftRepository.save(officerShift);
        } catch (Exception e) {
            log.error("Failed to delete police officer shift: {}", e.getMessage());
            String message = Utils.getExceptionMessage(e);
            return new Response<>(message);
        }
        return Response.success(officerShift);
    }

    public Response<OfficerShift> reassignShift(String shiftUid, String newOfficerUid) {
        if (shiftUid == null || newOfficerUid == null) {
            return Response.error("Shift UID and new officer UID are required");
        }

        Optional<OfficerShift> shiftOpt = officerShiftRepository.findByUid(shiftUid);
        if (shiftOpt.isEmpty()) return Response.error("Shift not found");

        OfficerShift originalShift = shiftOpt.get();
        LocalDate shiftDate = originalShift.getShiftDate();
        LocalTime startTime = originalShift.getStartTime();
        LocalTime endTime = originalShift.getEndTime();

        Optional<PoliceOfficer> officerOpt = policeOfficerRepository.findByUid(newOfficerUid);
        if (officerOpt.isEmpty()) return Response.error("New officer not found");

        // Adjacent shift rule
        boolean hasShiftBefore = officerShiftRepository.existsByOfficerUidAndShiftDate(newOfficerUid, shiftDate.minusDays(1));
        boolean hasShiftAfter = officerShiftRepository.existsByOfficerUidAndShiftDate(newOfficerUid, shiftDate.plusDays(1));

        if ((hasShiftBefore || hasShiftAfter) && !originalShift.getIsPunishmentMode()) {
            return Response.error("This officer has adjacent shifts. Reassignment not allowed.");
        }

        // Overlap validation
        List<OfficerShift> existingShifts =
                officerShiftRepository.findByOfficerUidAndShiftDate(newOfficerUid, shiftDate);

        for (OfficerShift existingShift : existingShifts) {
            if (isShiftOverlap(startTime, endTime,
                    existingShift.getStartTime(), existingShift.getEndTime())) {
                return Response.error("This officer has a conflicting shift on the same day.");
            }
        }

        // Step 1: Mark original shift
        if (!originalShift.getIsExcused()) {
            originalShift.setIsExcused(true);
            notifyShiftExcuse(originalShift);
        }
        originalShift.setIsReassigned(true);
        officerShiftRepository.save(originalShift);

        // Step 2: Create new shift
        OfficerShift newShift = new OfficerShift();
        newShift.setOfficer(officerOpt.get());
        newShift.setShiftDate(originalShift.getShiftDate());
        newShift.setShiftTime(originalShift.getShiftTime());
        newShift.setShiftDutyType(originalShift.getShiftDutyType());
        newShift.setDutyDescription(originalShift.getDutyDescription());
        newShift.setIsPunishmentMode(false);
        newShift.setReassignedFromUid(originalShift.getUid());
        newShift.setStartTime(startTime);
        newShift.setEndTime(endTime);

        newShift.setCheckpoint(originalShift.getCheckpoint());

        newShift.update();

        try {
            OfficerShift saved = officerShiftRepository.save(newShift);
            notifyShiftReassignment(saved);
            return Response.success(saved);
        } catch (Exception e) {
            return Response.error("Failed to reassign shift: " + Utils.getExceptionMessage(e));
        }
    }

    private boolean isShiftOverlap(LocalTime newStart, LocalTime newEnd, LocalTime existingStart, LocalTime existingEnd) {
        if (newEnd.isBefore(newStart)) {
            return existingStart.isAfter(newStart) || existingEnd.isBefore(newEnd) || existingEnd.equals(LocalTime.MIDNIGHT);
        }
        if (existingEnd.isBefore(existingStart)) {
            return newStart.isAfter(existingStart) || newEnd.isBefore(existingEnd) || newEnd.equals(LocalTime.MIDNIGHT);
        }
        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }
    public ResponsePage<OfficerShift> getPoliceOfficerShifts(PageableParam pageableParam) {
        return new ResponsePage<>(officerShiftRepository.getPoliceOfficerShifts(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key()));
    }

    public Response<OfficerShift> getPoliceOfficerShift(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<OfficerShift> optionalPoliceOfficerShift = officerShiftRepository.findByUid(uid);
        return optionalPoliceOfficerShift.map(Response::new).orElseGet(() -> new Response<>("Invalid police police officer shift provided"));
    }

    public ResponsePage<OfficerShift> getShiftsByStation(PageableParam pageableParam, String stationUid) {
        return new ResponsePage<>(officerShiftRepository.getShiftsByStation(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), stationUid));
    }

    public ResponsePage<OfficerShift> getShiftsByPoliceOfficer(PageableParam pageableParam, String policeOfficerUid) {
        return new ResponsePage<>(officerShiftRepository.getShiftsByPoliceOfficer(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), policeOfficerUid));
    }
    public Response<OfficerShift> getCurrentOfficerOnDuty(String stationUid) {
        log.info("🔍 Getting current officer on duty for station: {}", stationUid);

        if (stationUid == null || stationUid.trim().isEmpty()) {
            return Response.error("Station UID is required");
        }

        // Verify station exists
        Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(stationUid);
        if (!stationOpt.isPresent()) {
            return Response.error("Police station not found");
        }

        try {
            LocalDate today = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            log.info("Searching for shift on date: {} at time: {}", today, currentTime);

            // Find shift for today at this station
            List<OfficerShift> todayShifts = officerShiftRepository.findByStationAndDate(stationUid, today);
            log.info("Found {} shifts for today", todayShifts.size());

            // Filter for current time and on-duty officers
            Optional<OfficerShift> currentShift = todayShifts.stream()
                    .filter(shift -> {
                        // Skip OFF shifts
                        if ("OFF".equalsIgnoreCase(String.valueOf(shift.getShiftDutyType())))
                            return false;

                        // Skip excused shifts
                        if (shift.getIsExcused() != null && shift.getIsExcused()) {
                            return false;
                        }

                        // Check if current time is within shift hours
                        LocalTime startTime = shift.getStartTime();
                        LocalTime endTime = shift.getEndTime();

                        if (startTime == null || endTime == null) {
                            return false;
                        }

                        // Check if officer is on duty
                        PoliceOfficer officer = shift.getOfficer();
                        if (officer == null ) {
                            return false;
                        }

                        // Handle shifts that cross midnight
                        if (endTime.isBefore(startTime)) {
                            return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
                        } else {
                            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
                        }
                    })
                    .findFirst();

            if (currentShift.isPresent()) {
                OfficerShift shift = currentShift.get();
                log.info("Found officer on duty: {} ({})",
                        shift.getOfficer().getUserAccount().getName(),
                        shift.getShiftTime()
                );
                return Response.success(shift);
            }

            log.warn(" No officer currently on duty at this station");
            return Response.error("No officer currently on duty at this station");

        } catch (Exception e) {
            log.error("Error finding current officer on duty: {}", e.getMessage());
            return Response.error("Failed to find current officer: " + e.getMessage());
        }
    }

    public ResponseList<OfficerShift> getAllOfficersOnDutyNow(String stationUid) {
        log.info("Getting all officers on duty now");

        try {
            LocalDate today = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            List<OfficerShift> todayShifts;

            if (stationUid != null && !stationUid.trim().isEmpty()) {
                todayShifts = officerShiftRepository.findByStationAndDate(stationUid, today);
            } else {
                // Get all shifts for today
                todayShifts = officerShiftRepository.findByDate(today);
            }

            // Filter for officers currently on duty
            List<OfficerShift> onDutyShifts = todayShifts.stream()
                    .filter(shift -> {
                        if ("OFF".equalsIgnoreCase(String.valueOf(shift.getShiftDutyType()))) return false;
                        if (shift.getIsExcused() != null && shift.getIsExcused()) return false;

                        LocalTime startTime = shift.getStartTime();
                        LocalTime endTime = shift.getEndTime();
                        if (startTime == null || endTime == null) return false;

                        PoliceOfficer officer = shift.getOfficer();
                        if (officer == null) {
                            return false;
                        }

                        // Check time range
                        if (endTime.isBefore(startTime)) {
                            return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
                        } else {
                            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
                        }
                    })
                    .collect(Collectors.toList());

            log.info(" Found {} officers currently on duty", onDutyShifts.size());
            return new ResponseList<>(onDutyShifts);

        } catch (Exception e) {
            log.error(" Error finding officers on duty: {}", e.getMessage());
            return new ResponseList<>("Failed to find officers on duty: " + e.getMessage());
        }
    }
    private void notifyShiftAssignment(OfficerShift newShift) {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setTitle("Shift Assigned To You");
        notificationDto.setMessage("You have been assigned a shift on " + newShift.getShiftDate());
        notificationDto.setType(NotificationType.SHIFT_ASSIGNED);
        notificationDto.setChannels(List.of(NotificationChannel.IN_APP,NotificationChannel.PUSH));
        notificationDto.setRelatedEntityUid(newShift.getUid());
        notificationDto.setRelatedEntityType("SHIFT");
        notificationDto.setTargetUserUids(List.of(newShift.getOfficer().getUserAccount().getUid()));

        notificationService.sendNotification(notificationDto);
    }

    private void notifyShiftReassignment(OfficerShift newShift) {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setTitle("Shift Reassignment To You");
        notificationDto.setMessage("You have been Reassignment a shift on " + newShift.getShiftDate());
        notificationDto.setType(NotificationType.SHIFT_REASSIGNED);
        notificationDto.setChannels(List.of(NotificationChannel.IN_APP,NotificationChannel.PUSH));
        notificationDto.setRelatedEntityUid(newShift.getUid());
        notificationDto.setRelatedEntityType("SHIFT");
        notificationDto.setTargetUserUids(List.of(newShift.getOfficer().getUserAccount().getUid()));

        notificationService.sendNotification(notificationDto);
    }

    private void notifyShiftExcuse(OfficerShift newShift) {
        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setTitle("Shift Excused To You");
        notificationDto.setMessage("You have been excused a shift on " + newShift.getShiftDate());
        notificationDto.setType(NotificationType.SHIFT_EXCUSED);
        notificationDto.setChannels(List.of(NotificationChannel.IN_APP,NotificationChannel.PUSH));
        notificationDto.setRelatedEntityUid(newShift.getUid());
        notificationDto.setRelatedEntityType("SHIFT");
        notificationDto.setTargetUserUids(List.of(newShift.getOfficer().getUserAccount().getUid()));

        notificationService.sendNotification(notificationDto);
    }

    public ResponseList<OfficerShift> assignCheckpointShiftBulk(BulkCheckpointShiftDto dto) {

        if (dto == null) return new ResponseList<>("Data is required");
        if (dto.getOfficerUids() == null || dto.getOfficerUids().isEmpty())
            return new ResponseList<>("At least one officer is required");

        if (dto.getCheckpointUid() == null)
            return new ResponseList<>("Checkpoint is required");

        Optional<TrafficCheckpoint> cpOpt = trafficCheckpointRepository.findByUid(dto.getCheckpointUid());
        if (cpOpt.isEmpty()) return new ResponseList<>("Invalid checkpoint UID");

        TrafficCheckpoint checkpoint = cpOpt.get();

        List<OfficerShift> savedShifts = new ArrayList<>();

        try {

            for (String officerUid : dto.getOfficerUids()) {

                Optional<PoliceOfficer> officerOpt = policeOfficerRepository.findByUid(officerUid);
                if (officerOpt.isEmpty())
                    return new ResponseList<>("Officer not found: " + officerUid);

                PoliceOfficer officer = officerOpt.get();

                PoliceOfficer supervisor = checkpoint.getSupervisingOfficer();
                if (supervisor != null && supervisor.getUid().equals(officerUid)) {
                    return new ResponseList<>("Supervisor cannot be assigned as a checkpoint shift officer");
                }

                // Overlap validation
                List<OfficerShift> existingShifts =
                        officerShiftRepository.findByOfficerUidAndShiftDate(officerUid, dto.getShiftDate());

                for (OfficerShift existingShift : existingShifts) {
                    if (isShiftOverlap(dto.getStartTime(), dto.getEndTime(),
                            existingShift.getStartTime(), existingShift.getEndTime())) {
                        return new ResponseList<>("Officer " + officer.getBadgeNumber() + " has a conflicting shift");
                    }
                }

                // Create shift
                OfficerShift shift = new OfficerShift();
                shift.setOfficer(officer);
                shift.setShiftDate(dto.getShiftDate());
                shift.setShiftTime(dto.getShiftTime());
                shift.setShiftDutyType(dto.getShiftDutyType());
                shift.setStartTime(dto.getStartTime());
                shift.setEndTime(dto.getEndTime());
                shift.setCheckpoint(checkpoint);
                shift.update();

                OfficerShift saved = officerShiftRepository.save(shift);
                savedShifts.add(saved);

                notifyShiftAssignment(saved);
            }

            return new ResponseList<>(savedShifts);

        } catch (Exception e) {
            log.error("Failed to assign bulk checkpoint shifts: {}", e.getMessage());
            return new ResponseList<>("Failed to assign checkpoint shifts: " + Utils.getExceptionMessage(e));
        }
    }

    public ResponsePage<OfficerShift> getPoliceOfficerShiftsByCheckpoint(PageableParam pageableParam, String checkpointUid) {
        return new ResponsePage<>(officerShiftRepository.getPoliceOfficerShiftsByCheckpoint(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), checkpointUid));
    }
}

