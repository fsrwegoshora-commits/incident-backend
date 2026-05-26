package com.smartincident.incidentbackend.classification.dto;

import com.smartincident.incidentbackend.enums.EmergencyCategory;
import com.smartincident.incidentbackend.enums.EmergencyLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ClassificationResult {

    /** Recommended emergency category (which agencies need to respond). */
    private EmergencyCategory recommendedCategory;

    /** Recommended severity level. */
    private EmergencyLevel recommendedLevel;

    /** Keywords detected in the text that drove this classification. */
    private List<String> detectedKeywords;

    /** Agency codes recommended to respond (e.g. "POLICE", "FIRE", "MEDICAL"). */
    private List<String> recommendedAgencies;

    /** Confidence scores per agency (0.0 – 1.0). */
    private Map<String, Double> confidenceScores;

    /** Human-readable reason for the classification. */
    private String reasoning;
}
