package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
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

    public Response<OfficerShift> saveShift(OfficerShiftDto dto) {
        if (dto == null) return Response.error("Shift DTO cannot be null");

        OfficerShift shift = new OfficerShift();
        if (dto.getUid() != null) {
            Optional<OfficerShift> existing = officerShiftRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Invalid shift UID");
            shift = existing.get();
        }

        if (dto.getOfficerUid() == null) return Response.error("Officer UID is required");
        if (dto.getShiftDate() == null) return Response.error("Shift date is required");
        if (dto.getShiftType() == null) return Response.error("Shift type is required");
        if(dto.getStartTime() ==null) return Response.error("start time is required");
        if(dto.getEndTime() ==null) return Response.error("end time is required");

        Optional<PoliceOfficer> officerOpt = policeOfficerRepository.findByUid(dto.getOfficerUid());
        if (officerOpt.isEmpty()) return Response.error("Officer not found");

        shift.setOfficer(officerOpt.get());
        shift.setShiftDate(dto.getShiftDate());
        shift.setShiftType(dto.getShiftType());
        shift.setDutyDescription(dto.getDutyDescription());
        shift.setIsPunishmentMode(dto.getIsPunishmentMode());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());

        if (dto.getUid() != null) {
            shift.update();
        }

        try {
            OfficerShift saved = officerShiftRepository.save(shift);
            log.info("Saved shift for officer {} on {}", saved.getOfficer().getBadgeNumber(), saved.getShiftDate());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save shift: {}", e.getMessage());
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

        // Check for adjacent shifts (date-based)
        boolean hasShiftBefore = officerShiftRepository.existsByOfficerUidAndShiftDate(newOfficerUid, shiftDate.minusDays(1));
        boolean hasShiftAfter = officerShiftRepository.existsByOfficerUidAndShiftDate(newOfficerUid, shiftDate.plusDays(1));

        if ((hasShiftBefore || hasShiftAfter) && !originalShift.getIsPunishmentMode()) {
            return Response.error("This officer has adjacent shifts. Reassignment not allowed.");
        }

        // Check for overlapping shifts on the same day
        List<OfficerShift> existingShifts = officerShiftRepository.findByOfficerUidAndShiftDate(newOfficerUid, shiftDate);
        for (OfficerShift existingShift : existingShifts) {
            if (isShiftOverlap(startTime, endTime, existingShift.getStartTime(), existingShift.getEndTime())) {
                return Response.error("This officer has a conflicting shift on the same day.");
            }
        }

        // Step 1: Mark original shift as excused + reassigned
        originalShift.setIsExcused(true);
        originalShift.setIsReassigned(true);
        officerShiftRepository.save(originalShift);

        // Step 2: Create new shift for new officer
        OfficerShift newShift = new OfficerShift();
        newShift.setOfficer(officerOpt.get());
        newShift.setShiftDate(originalShift.getShiftDate());
        newShift.setShiftType(originalShift.getShiftType());
        newShift.setDutyDescription(originalShift.getDutyDescription());
        newShift.setIsPunishmentMode(false);
        newShift.setReassignedFromUid(originalShift.getUid());
        newShift.setStartTime(startTime); // Set start time
        newShift.setEndTime(endTime); // Set end time
        newShift.update();

        try {
            OfficerShift saved = officerShiftRepository.save(newShift);
            log.info("Reassigned shift from {} to {}", originalShift.getOfficer().getBadgeNumber(), saved.getOfficer().getBadgeNumber());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save reassigned shift: {}", e.getMessage());
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
                        if ("OFF".equalsIgnoreCase(String.valueOf(shift.getShiftType())))
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
                        shift.getShiftType()
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
                        if ("OFF".equalsIgnoreCase(String.valueOf(shift.getShiftType()))) return false;
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
}

