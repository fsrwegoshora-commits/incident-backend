package com.smartincident.incidentbackend.classification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartincident.incidentbackend.enums.IncidentNature;
import com.smartincident.incidentbackend.enums.ReportLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI-powered incident triage.
 *
 * Primary:  Claude Haiku via Anthropic API — handles English, Swahili, and mixed-language reports.
 * Fallback: Rule-based keyword engine when the API key is not configured or the call fails.
 */
@Service
@Slf4j
public class AiTriageService {

    private static final String ANTHROPIC_API = "https://api.anthropic.com/v1/messages";
    private static final String MODEL         = "claude-haiku-4-5-20251001";

    @Value("${anthropic.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper       = new ObjectMapper();

    // ── Swahili emergency indicators ──────────────────────────────────────────
    private static final Set<String> SW_EMERGENCY = Set.of(
            "sasa", "hivi sasa", "inatokea", "inaendelea", "kuna ajali",
            "kuna moto", "kuna wizi", "kuna mapigano", "msaada", "haraka",
            "dharura", "anakufa", "anauma sana", "amepigwa risasi",
            "amepoltezwa", "anapigwa", "mnasaidia", "mtu ameuawa",
            "mtu amekufa", "kuna damu", "damu nyingi", "moto mkubwa"
    );

    // ── Swahili non-emergency / past-tense indicators ─────────────────────────
    private static final Set<String> SW_NON_EMERGENCY = Set.of(
            "jana", "juzi", "wiki iliyopita", "mwezi uliopita", "mwaka uliopita",
            "nilikuwa", "nilipigwa", "nilibiwa", "nilipoteza", "ilikuwa",
            "waliiba", "walipiga", "ombi", "lalamiko", "taarifa ya",
            "kutaka kuchunguza", "kutaka kuripoti", "nataka kuripoti"
    );

    // ── Swahili language markers (for detection) ──────────────────────────────
    private static final Set<String> SW_MARKERS = Set.of(
            "kuna", "nina", "nilikuwa", "niko", "watu", "moto", "ajali",
            "msaada", "tafadhali", "naomba", "nimepoteza", "amebiwa",
            "polisi", "hospitali", "ambulansi", "sasa", "jana", "leo"
    );

    // ── English non-emergency indicators ─────────────────────────────────────
    private static final Set<String> EN_NON_EMERGENCY = Set.of(
            "yesterday", "last week", "last month", "last year", "days ago",
            "weeks ago", "months ago", "previously", "in the past", "occurred on",
            "happened on", "was stolen", "was robbed", "was assaulted", "was attacked",
            "complaint", "investigation", "report a", "want to report",
            "missing since", "missing for", "fraud", "scam", "lost my", "lost property"
    );

    // ── English emergency indicators ──────────────────────────────────────────
    private static final Set<String> EN_EMERGENCY = Set.of(
            "right now", "happening now", "currently", "ongoing", "active",
            "on fire", "is burning", "just happened", "just now", "in progress",
            "someone is", "they are", "he is", "she is", "we are", "help now",
            "emergency", "urgent", "immediately", "bleeding", "unconscious"
    );

    // ── Public API ─────────────────────────────────────────────────────────────

    public record TriageResult(
            IncidentNature nature,
            ReportLanguage language,
            double confidence,
            String reasoning
    ) {}

    /**
     * Triage an incident report. Uses Claude if API key is set, rule-based otherwise.
     */
    public TriageResult triage(String title, String description) {
        String text = (title == null ? "" : title) + " " + (description == null ? "" : description);
        text = text.trim();

        if (!apiKey.isBlank()) {
            try {
                return triageWithClaude(title, description);
            } catch (Exception e) {
                log.warn("Claude triage failed ({}), falling back to rule-based", e.getMessage());
            }
        }
        return ruleBased(text);
    }

    // ── Claude API ─────────────────────────────────────────────────────────────

    private TriageResult triageWithClaude(String title, String description) throws Exception {
        String prompt = """
                You are an emergency response triage AI for a police dispatch system in Tanzania.

                Analyze this incident report and classify it. The report may be in English, Swahili, or a mix of both.

                Title: %s
                Description: %s

                Classification rules:
                - EMERGENCY: incident is happening RIGHT NOW or requires immediate police/medical/fire response
                - NON_EMERGENCY: historical report, past event, complaint, fraud, missing person (unless just happened), investigation request

                Key Swahili time indicators:
                - EMERGENCY: sasa (now), hivi sasa (right now), inatokea (happening), kuna + incident word
                - NON_EMERGENCY: jana (yesterday), wiki iliyopita (last week), nilikuwa (I was), ilikuwa (it was)

                Respond ONLY with valid JSON — no explanation, no markdown:
                {"nature":"EMERGENCY" or "NON_EMERGENCY","language":"ENGLISH" or "SWAHILI" or "MIXED","confidence":0.0-1.0,"reasoning":"one short sentence in English"}
                """.formatted(
                        title != null ? title : "",
                        description != null ? description : ""
                );

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", 300,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key",           apiKey);
        headers.set("anthropic-version",   "2023-06-01");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(ANTHROPIC_API, request, String.class);

        JsonNode root     = mapper.readTree(response.getBody());
        String   jsonText = root.path("content").get(0).path("text").asText();

        // Strip any markdown code fences if present
        jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();
        JsonNode result = mapper.readTree(jsonText);

        return new TriageResult(
                IncidentNature.valueOf(result.path("nature").asText("EMERGENCY")),
                ReportLanguage.valueOf(result.path("language").asText("ENGLISH")),
                result.path("confidence").asDouble(0.85),
                result.path("reasoning").asText("")
        );
    }

    // ── Rule-based fallback (English + Swahili) ────────────────────────────────

    private TriageResult ruleBased(String text) {
        String lower = text.toLowerCase();

        ReportLanguage lang = detectLanguage(lower);

        // Count emergency vs non-emergency signals
        long emergencySignals    = countMatches(lower, EN_EMERGENCY) + countMatches(lower, SW_EMERGENCY);
        long nonEmergencySignals = countMatches(lower, EN_NON_EMERGENCY) + countMatches(lower, SW_NON_EMERGENCY);

        IncidentNature nature;
        String reasoning;
        double confidence;

        if (nonEmergencySignals > emergencySignals) {
            nature     = IncidentNature.NON_EMERGENCY;
            reasoning  = "Past-tense or historical indicators detected — routed to investigation queue.";
            confidence = Math.min(0.9, 0.6 + (nonEmergencySignals * 0.1));
        } else if (emergencySignals > 0) {
            nature     = IncidentNature.EMERGENCY;
            reasoning  = "Active/ongoing emergency indicators detected — routed to dispatch queue.";
            confidence = Math.min(0.9, 0.6 + (emergencySignals * 0.1));
        } else {
            // No strong signal — default to EMERGENCY to be safe
            nature     = IncidentNature.EMERGENCY;
            reasoning  = "No clear time indicators found — classified as emergency by default.";
            confidence = 0.55;
        }

        return new TriageResult(nature, lang, confidence, reasoning);
    }

    private ReportLanguage detectLanguage(String lower) {
        long swWords = countMatches(lower, SW_MARKERS);
        // Count rough English word presence by checking for common short English words
        boolean hasEnglish = lower.matches(".*\\b(the|is|are|was|there|has|have|at|in|on|a|an|it|he|she|we|they)\\b.*");
        boolean hasSwahili = swWords >= 2;

        if (hasEnglish && hasSwahili) return ReportLanguage.MIXED;
        if (hasSwahili)               return ReportLanguage.SWAHILI;
        return ReportLanguage.ENGLISH;
    }

    private long countMatches(String text, Set<String> keywords) {
        return keywords.stream().filter(text::contains).count();
    }
}
