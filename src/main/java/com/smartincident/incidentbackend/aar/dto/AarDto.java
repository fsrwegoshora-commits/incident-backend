package com.smartincident.incidentbackend.aar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AarDto {
    private String summary;
    private String whatWorked;
    private String areasForImprovement;
    private String recommendedImprovements;
}
