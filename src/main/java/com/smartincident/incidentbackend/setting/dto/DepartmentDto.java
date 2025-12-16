package com.smartincident.incidentbackend.setting.dto;

import com.smartincident.incidentbackend.enums.DepartmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDto {
    private String uid;
    private String name;
    private DepartmentType type;
    private String agencyUid;
}
