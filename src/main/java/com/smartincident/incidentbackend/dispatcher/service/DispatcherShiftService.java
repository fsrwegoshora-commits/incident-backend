package com.smartincident.incidentbackend.dispatcher.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.dispatcher.dto.DispatcherShiftDto;
import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherShiftService {

    private final DispatcherShiftRepository dispatcherShiftRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public Response<DispatcherShift> saveShift(DispatcherShiftDto dto) {
        if (dto == null) return Response.error("Shift data is required");
        if (dto.getDispatcherUid() == null) return Response.error("Dispatcher UID is required");
        if (dto.getShiftDate() == null) return Response.error("Shift date is required");
        if (dto.getShiftTime() == null) return Response.error("Shift time is required");
        if (dto.getStartTime() == null) return Response.error("Start time is required");
        if (dto.getEndTime() == null) return Response.error("End time is required");

        Optional<User> userOpt = userRepository.findByUid(dto.getDispatcherUid());
        if (userOpt.isEmpty()) return Response.error("Dispatcher not found");

        User dispatcher = userOpt.get();
        if (dispatcher.getRole() != Role.DISPATCHER && dispatcher.getRole() != Role.DISPATCHER_SUPERVISOR) {
            return Response.error("User is not a dispatcher or supervisor");
        }

        DispatcherShift shift = new DispatcherShift();

        if (dto.getUid() != null) {
            Optional<DispatcherShift> existing = dispatcherShiftRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Invalid shift UID");
            shift = existing.get();
        } else {
            // Overlap check only for new shifts
            List<DispatcherShift> sameDay = dispatcherShiftRepository
                    .findByDispatcherUidAndIsDeletedFalse(dto.getDispatcherUid())
                    .stream()
                    .filter(s -> s.getShiftDate().equals(dto.getShiftDate()))
                    .toList();

            for (DispatcherShift existing : sameDay) {
                if (isOverlap(dto.getStartTime(), dto.getEndTime(), existing.getStartTime(), existing.getEndTime())) {
                    return Response.error("Dispatcher already has a conflicting shift on this date");
                }
            }
        }

        shift.setDispatcher(dispatcher);
        shift.setShiftDate(dto.getShiftDate());
        shift.setShiftTime(dto.getShiftTime());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setNotes(dto.getNotes());
        shift.update();

        try {
            DispatcherShift saved = dispatcherShiftRepository.save(shift);
            notifyShiftAssigned(saved);
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save dispatcher shift: {}", e.getMessage());
            return Response.error("Failed to save shift: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<DispatcherShift> deleteShift(String uid) {
        if (uid == null) return Response.error("Shift UID is required");
        Optional<DispatcherShift> opt = dispatcherShiftRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Shift not found");
        DispatcherShift shift = opt.get();
        if (shift.getIsDeleted()) return Response.error("Shift already deleted");
        shift.delete();
        try {
            dispatcherShiftRepository.save(shift);
            return Response.success(shift);
        } catch (Exception e) {
            log.error("Failed to delete dispatcher shift: {}", e.getMessage());
            return Response.error("Failed to delete shift: " + Utils.getExceptionMessage(e));
        }
    }

    public ResponseList<DispatcherShift> getShiftsByDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        return new ResponseList<>(dispatcherShiftRepository.findByShiftDateAndIsDeletedFalse(date));
    }

    public ResponseList<DispatcherShift> getShiftsByDispatcher(String dispatcherUid) {
        if (dispatcherUid == null) return ResponseList.error("Dispatcher UID is required");
        return new ResponseList<>(dispatcherShiftRepository.findByDispatcherUidAndIsDeletedFalse(dispatcherUid));
    }

    public ResponseList<DispatcherShift> getShiftsByDispatchCenter(String centerUid) {
        if (centerUid == null) return ResponseList.error("Dispatch center UID is required");
        return new ResponseList<>(dispatcherShiftRepository.findByDispatchCenterUid(centerUid));
    }

    public ResponseList<DispatcherShift> getOnDutyNow() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return new ResponseList<>(dispatcherShiftRepository.findOnDutyNow(today, now));
    }

    private boolean isOverlap(LocalTime newStart, LocalTime newEnd, LocalTime existStart, LocalTime existEnd) {
        if (newEnd.isBefore(newStart)) {
            return existStart.isAfter(newStart) || existEnd.isBefore(newEnd);
        }
        if (existEnd.isBefore(existStart)) {
            return newStart.isAfter(existStart) || newEnd.isBefore(existEnd);
        }
        return newStart.isBefore(existEnd) && newEnd.isAfter(existStart);
    }

    private void notifyShiftAssigned(DispatcherShift shift) {
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Dispatcher Shift Assigned");
        dto.setMessage("You have been assigned a dispatcher shift on " + shift.getShiftDate()
                + " (" + shift.getShiftTime().name() + ")");
        dto.setType(NotificationType.SHIFT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(shift.getUid());
        dto.setRelatedEntityType("DISPATCHER_SHIFT");
        dto.setTargetUserUids(List.of(shift.getDispatcher().getUid()));
        notificationService.sendNotification(dto);
    }
}
