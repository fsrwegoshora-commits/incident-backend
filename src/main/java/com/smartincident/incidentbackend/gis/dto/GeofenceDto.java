package com.smartincident.incidentbackend.gis.dto;

import com.smartincident.incidentbackend.enums.GeofenceType;
import lombok.Data;

@Data
public class GeofenceDto {
    private String name;
    private String description;
    private GeofenceType type;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusMetres;
    private String agencyUid;
    private Boolean alertOnEntry;
    private Boolean alertOnExit;
}
