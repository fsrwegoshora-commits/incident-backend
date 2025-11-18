package com.smartincident.incidentbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.warn("FIREBASE BEAN INAANZA KWANZA KABISA!");

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase tayari imeanza, narudisha ile iliyopo");
            return FirebaseApp.getInstance();
        }

        var resource = new ClassPathResource("firebase-service-account.json");
        if (!resource.exists()) {
            log.error("firebase-service-account.json HAIPO! Weka kwenye src/main/resources/");
            throw new RuntimeException("Firebase config file missing!");
        }

        log.info("Imepata file → {}", resource.getURL());

        var credentials = GoogleCredentials.fromStream(resource.getInputStream());
        var options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.warn("FIREBASE IMEANZA VIZURI KAMA SPRING BEAN!");
        log.warn("Project ID: {}", app.getOptions().getProjectId());

        return app;
    }
}