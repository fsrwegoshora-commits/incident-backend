package com.smartincident.incidentbackend.controller;

import com.google.firebase.FirebaseApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
public class FirebaseDebugController {

    @GetMapping("/firebase-status")
    public Map<String, Object> getFirebaseStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Check Firebase Apps
            boolean firebaseAppsEmpty = FirebaseApp.getApps().isEmpty();
            status.put("firebaseAppsEmpty", firebaseAppsEmpty);
            status.put("firebaseAppsCount", FirebaseApp.getApps().size());

            // Check if config file exists
            String configPath = "firebase-service-account.json";
            Resource resource = new ClassPathResource(configPath);
            boolean configExists = resource.exists();
            status.put("configFileExists", configExists);
            status.put("configFilePath", configPath);

            if (configExists) {
                status.put("configFileURL", resource.getURL().toString());
                status.put("configFileDescription", resource.getDescription());
            }

            // Firebase App details
            if (!firebaseAppsEmpty) {
                FirebaseApp app = FirebaseApp.getInstance();
                status.put("firebaseAppName", app.getName());
                status.put("firebaseProjectId", app.getOptions().getProjectId());
                status.put("status", "INITIALIZED");
            } else {
                status.put("status", "NOT_INITIALIZED");
                status.put("reason", "FirebaseApp.getApps() is empty");
            }

            log.info("🔍 Firebase Status: {}", status);

        } catch (Exception e) {
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            log.error("❌ Firebase status check error: {}", e.getMessage(), e);
        }

        return status;
    }

    @GetMapping("/check-resources")
    public Map<String, Object> checkResources() {
        Map<String, Object> resources = new HashMap<>();

        try {
            // Check common Firebase config file names
            String[] possibleFileNames = {
                    "firebase-service-account.json",
                    "firebase-adminsdk.json",
                    "service-account-key.json",
                    "google-services.json"
            };

            for (String fileName : possibleFileNames) {
                Resource resource = new ClassPathResource(fileName);
                resources.put(fileName, resource.exists());
                if (resource.exists()) {
                    resources.put(fileName + "_url", resource.getURL().toString());
                }
            }

            log.info("📁 Resource check: {}", resources);

        } catch (Exception e) {
            resources.put("error", e.getMessage());
        }

        return resources;
    }
}