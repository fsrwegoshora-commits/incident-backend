package com.smartincident.incidentbackend.gis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDto {

    private int totalIncidents;
    private int days;
    private double gridSizeDegrees;
    private List<HeatCell> cells;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatCell {
        /** Grid cell center latitude. */
        private double latitude;
        /** Grid cell center longitude. */
        private double longitude;
        private int count;
        private int intensity; // 1–5 normalized
        private Map<String, Long> byType;
        private Map<String, Long> byLevel;
    }
}
