package com.smartincident.incidentbackend.police.dto;

import lombok.Data;

@Data
public class OfficerProfileUpdateDto {
    private String name;
    private String username;
    private String password; // plain-text, optional — encoded before saving
}
