package com.smartincident.incidentbackend.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyDto {
    private String uid;
    private String name;
    private String code;
    private String description;
}
