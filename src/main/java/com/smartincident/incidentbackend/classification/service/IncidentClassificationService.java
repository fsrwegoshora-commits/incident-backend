package com.smartincident.incidentbackend.classification.service;

import com.smartincident.incidentbackend.classification.dto.ClassificationResult;
import com.smartincident.incidentbackend.enums.EmergencyCategory;
import com.smartincident.incidentbackend.enums.EmergencyLevel;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Keyword-based incident classification.
 * Analyzes free-text title + description and recommends:
 *  - Which agencies need to respond (EmergencyCategory)
 *  - Severity level (EmergencyLevel)
 *  - Confidence scores per agency
 */
@Service
public class IncidentClassificationService {

    // ── Keyword lists ──────────────────────────────────────────────────────────

    private static final Set<String> POLICE_KEYWORDS = Set.of(
            "theft", "steal", "stolen", "rob", "robbery", "assault", "attack",
            "fight", "weapon", "gun", "knife", "pistol", "criminal", "crime",
            "murder", "killed", "kill", "shot", "shooting", "arrested", "suspect",
            "violence", "violent", "burglary", "break-in", "vandalism", "fraud",
            "scam", "kidnap", "kidnapping", "abduct", "missing", "traffic",
            "reckless", "drunk", "drugs", "drug", "gang", "threat", "harassment",
            "domestic", "trespass", "looting"
    );

    private static final Set<String> FIRE_KEYWORDS = Set.of(
            "fire", "smoke", "burning", "burn", "burnt", "explosion", "explode",
            "blast", "fumes", "chemical", "gas leak", "hazmat", "wildfire", "arson",
            "flames", "ignite", "combustion", "inferno", "blaze"
    );

    private static final Set<String> MEDICAL_KEYWORDS = Set.of(
            "medical", "ambulance", "hospital", "injured", "injury", "accident",
            "blood", "bleeding", "unconscious", "breathing", "heart", "chest pain",
            "seizure", "stroke", "baby", "birth", "overdose", "collapse", "fall",
            "fainted", "faint", "choking", "allergic", "diabetic", "fracture",
            "broken", "drown", "shock", "unresponsive", "ems", "paramedic", "dead",
            "dying", "critical", "emergency", "pain", "stabbed", "shot"
    );

    // ── Severity escalators ───────────────────────────────────────────────────

    private static final Set<String> CRITICAL_WORDS = Set.of(
            "explosion", "blast", "mass", "multiple victims", "building fire",
            "hostage", "terrorist", "bombing", "plane", "crash", "chemical spill"
    );

    private static final Set<String> HIGH_WORDS = Set.of(
            "fire", "shooting", "shot", "gun", "stabbed", "unconscious", "unresponsive",
            "dead", "dying", "kidnap", "kidnapping", "abduct", "overdose", "collapsed"
    );

    private static final Set<String> LOW_WORDS = Set.of(
            "traffic violation", "minor", "theft", "vandalism", "fraud", "suspicious",
            "loitering", "noise", "trespass"
    );

    // ── Public API ─────────────────────────────────────────────────────────────

    public ClassificationResult classify(String title, String description) {
        String combined = normalize(title + " " + (description != null ? description : ""));

        Map<String, Double> scores = new LinkedHashMap<>();
        List<String> detected = new ArrayList<>();

        double policeScore  = scoreAgency(combined, POLICE_KEYWORDS,  detected);
        double fireScore    = scoreAgency(combined, FIRE_KEYWORDS,     detected);
        double medicalScore = scoreAgency(combined, MEDICAL_KEYWORDS,  detected);

        double total = policeScore + fireScore + medicalScore;
        if (total > 0) {
            scores.put("POLICE",  round(policeScore  / total));
            scores.put("FIRE",    round(fireScore    / total));
            scores.put("MEDICAL", round(medicalScore / total));
        } else {
            scores.put("POLICE", 0.33);
            scores.put("FIRE",   0.33);
            scores.put("MEDICAL", 0.34);
        }

        boolean needsPolice  = policeScore  > 0;
        boolean needsFire    = fireScore    > 0;
        boolean needsMedical = medicalScore > 0;

        // If nothing matched, default to police
        if (!needsPolice && !needsFire && !needsMedical) {
            needsPolice = true;
        }

        EmergencyCategory category = resolveCategory(needsPolice, needsFire, needsMedical);
        EmergencyLevel    level    = resolveLevel(combined);
        List<String>      agencies = buildAgencyList(needsPolice, needsFire, needsMedical);
        String            reason   = buildReason(agencies, detected, level);

        return ClassificationResult.builder()
                .recommendedCategory(category)
                .recommendedLevel(level)
                .detectedKeywords(detected.stream().distinct().limit(10).toList())
                .recommendedAgencies(agencies)
                .confidenceScores(scores)
                .reasoning(reason)
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
    }

    private double scoreAgency(String text, Set<String> keywords, List<String> detected) {
        double score = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                detected.add(kw);
                score += 1.0;
            }
        }
        return score;
    }

    private EmergencyCategory resolveCategory(boolean p, boolean f, boolean m) {
        if (p && f && m) return EmergencyCategory.ALL_THREE;
        if (p && f)      return EmergencyCategory.POLICE_FIRE;
        if (p && m)      return EmergencyCategory.POLICE_MEDICAL;
        if (f && m)      return EmergencyCategory.MEDICAL_FIRE;
        if (m)           return EmergencyCategory.MEDICAL_ONLY;
        if (f)           return EmergencyCategory.FIRE_ONLY;
        return EmergencyCategory.POLICE_ONLY;
    }

    private EmergencyLevel resolveLevel(String text) {
        for (String w : CRITICAL_WORDS) {
            if (text.contains(w)) return EmergencyLevel.CRITICAL;
        }
        for (String w : HIGH_WORDS) {
            if (text.contains(w)) return EmergencyLevel.HIGH;
        }
        for (String w : LOW_WORDS) {
            if (text.contains(w)) return EmergencyLevel.LOW;
        }
        return EmergencyLevel.MEDIUM;
    }

    private List<String> buildAgencyList(boolean p, boolean f, boolean m) {
        List<String> list = new ArrayList<>();
        if (p) list.add("POLICE");
        if (m) list.add("MEDICAL");
        if (f) list.add("FIRE");
        return list;
    }

    private String buildReason(List<String> agencies, List<String> keywords, EmergencyLevel level) {
        String kws = keywords.isEmpty() ? "general context" : String.join(", ", keywords.stream().distinct().limit(5).toList());
        return String.format("Detected %s-level incident requiring %s response. Keywords: %s.",
                level.name().toLowerCase(), String.join(" + ", agencies), kws);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
