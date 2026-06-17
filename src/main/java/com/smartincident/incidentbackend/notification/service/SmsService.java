package com.smartincident.incidentbackend.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/** Sends SMS via the Africa's Talking messaging API. */
@Slf4j
@Service
public class SmsService {

    @Value("${africastalking.username:sandbox}")
    private String username;

    @Value("${africastalking.api-key:}")
    private String apiKey;

    @Value("${africastalking.sender-id:}")
    private String senderId;

    @Value("${africastalking.base-url:https://api.sandbox.africastalking.com/version1/messaging}")
    private String baseUrl;

    @Value("${app.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${app.sms.default-country-code:255}")
    private String defaultCountryCode;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Sends an SMS to the given phone number. Returns true only on a confirmed provider "Success" status. */
    public boolean sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS sending disabled via config — skipping send to {}", mask(phoneNumber));
            return false;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Africa's Talking API key not configured — cannot send SMS to {}", mask(phoneNumber));
            return false;
        }

        String to = normalize(phoneNumber);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("apiKey", apiKey);
            headers.set("Accept", "application/json");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("to", to);
            body.add("message", message);
            if (senderId != null && !senderId.isBlank()) {
                body.add("from", senderId);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            JsonNode recipients = mapper.readTree(response.getBody())
                    .path("SMSMessageData").path("Recipients");

            if (recipients.isArray() && !recipients.isEmpty()) {
                JsonNode recipient = recipients.get(0);
                String status = recipient.path("status").asText("");
                if ("Success".equalsIgnoreCase(status)) {
                    log.info("SMS sent to {} (messageId={})", mask(to), recipient.path("messageId").asText(""));
                    return true;
                }
                log.warn("SMS to {} failed: {}", mask(to), status);
                return false;
            }

            log.warn("Unexpected Africa's Talking response for {}: {}", mask(to), response.getBody());
            return false;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", mask(to), e.getMessage());
            return false;
        }
    }

    /** Converts local Tanzanian numbers (0xxxxxxxxx) to E.164 (+255xxxxxxxxx); leaves already-international numbers untouched. */
    private String normalize(String phoneNumber) {
        String trimmed = phoneNumber.trim();
        if (trimmed.startsWith("+")) return trimmed;
        if (trimmed.startsWith("0")) return "+" + defaultCountryCode + trimmed.substring(1);
        if (trimmed.startsWith(defaultCountryCode)) return "+" + trimmed;
        return "+" + trimmed;
    }

    private String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) return "****";
        return "*".repeat(phoneNumber.length() - 4) + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
