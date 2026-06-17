package com.smartincident.incidentbackend.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveResourceDto {

    private List<VehicleMarker> vehicles;
    private List<IncidentMarker> activeIncidents;
    private List<UnitMarker> units;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleMarker {
        private String uid;
        private String plateNumber;
        private String vehicleType;
        private String status;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private LocalDateTime lastUpdated;
        private String activeIncidentUid;
        private String activeIncidentTitle;
        private String unitName;
        private String agencyType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentMarker {
        private String uid;
        private String title;
        private String type;
        private String status;
        private String emergencyLevel;
        private Double latitude;
        private Double longitude;
        private LocalDateTime reportedAt;
        private Boolean slaBreached;
        private String assignedUnitName;
        private String assignedStationName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitMarker {
        private String uid;
        private String name;
        private String unitType;
        private String agencyType;
        private Double latitude;
        private Double longitude;
        private int onDutyPersonnel;
        private int availableVehicles;
        private int activeIncidents;
    }
}
