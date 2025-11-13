package com.smartincident.incidentbackend.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${sms.provider.url:}")
    private String smsProviderUrl;

    @Value("${sms.provider.api-key:}")
    private String apiKey;

    public boolean sendSms(String phoneNumber, String message) {
        try {
            // Integrate with your SMS provider (AfricasTalking, Twilio, etc.)
            log.info("SENDING SMS to {}: {}", phoneNumber, message);

            // Example implementation:
            // RestTemplate restTemplate = new RestTemplate();
            // HttpHeaders headers = new HttpHeaders();
            // headers.set("APIKey", apiKey);
            // 
            // SmsRequest smsRequest = new SmsRequest(phoneNumber, message);
            // HttpEntity<SmsRequest> request = new HttpEntity<>(smsRequest, headers);
            // 
            // ResponseEntity<String> response = restTemplate.postForEntity(
            //     smsProviderUrl, request, String.class);
            // 
            // return response.getStatusCode().is2xxSuccessful();

            // For now, just log and return true for testing
            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }
}