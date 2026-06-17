package com.smartincident.incidentbackend.availability.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ForceAvailabilityDto {

    // Vehicle counts by status
    private long vehiclesTotal;
    private long vehiclesAvailable;
    private long vehiclesDispatched;
    private long vehiclesOnScene;
    private long vehiclesReturning;
    private long vehiclesMaintenance;
    private Map<String, Long> vehiclesByStatus;

    // Officer shifts active today
    private long officersOnShiftToday;
    private long officersOnDuty;

    // Vehicle shifts active today
    private long vehicleShiftsActive;

    // Operational posts
    private long operationalPostsActive;

    // Per-station/unit breakdown
    private List<UnitSummary> units;

    @Data
    @Builder
    public static class UnitSummary {
        private String uid;
        private String name;
        private String type;
        private long vehiclesAvailable;
        private long vehiclesTotal;
        private long officersOnDuty;
        private long activeShifts;
    }
}
