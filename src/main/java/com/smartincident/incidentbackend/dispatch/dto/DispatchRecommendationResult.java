package com.smartincident.incidentbackend.dispatch.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Full response from the smart dispatch recommendation engine.
 * Contains the nearest police station, fire unit, and medical unit,
 * plus any available vehicles from those units.
 */
@Data
@Builder
public class DispatchRecommendationResult {

    private String incidentUid;
    private Double incidentLatitude;
    private Double incidentLongitude;

    /** Nearest police station (null if incident doesn't require police). */
    private UnitRecommendation nearestPoliceStation;

    /** Nearest fire unit (null if not required). */
    private UnitRecommendation nearestFireUnit;

    /** Nearest medical / ambulance unit (null if not required). */
    private UnitRecommendation nearestMedicalUnit;

    /** Top 5 available vehicles across all nearest units. */
    private List<VehicleRecommendation> availableVehicles;

    /** Total available vehicles across all recommended units. */
    private int totalAvailableVehicles;
}
