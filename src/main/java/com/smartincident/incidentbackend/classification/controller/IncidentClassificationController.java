package com.smartincident.incidentbackend.classification.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.classification.dto.ClassificationResult;
import com.smartincident.incidentbackend.classification.service.IncidentClassificationService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * POST /api/incidents/classify
 * Accepts free-text title + description and returns AI classification recommendations.
 * No special permission — any authenticated user (citizen or officer) can call it.
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentClassificationController {

    private final IncidentClassificationService classificationService;

    @Authenticated
    @PostMapping("/classify")
    public Response<ClassificationResult> classify(@RequestBody Map<String, String> body) {
        String title       = body.getOrDefault("title", "");
        String description = body.getOrDefault("description", "");

        if (title.isBlank() && description.isBlank()) {
            return Response.error("Provide at least a title or description to classify");
        }

        ClassificationResult result = classificationService.classify(title, description);
        return Response.success(result);
    }
}
