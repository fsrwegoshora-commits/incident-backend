package com.smartincident.incidentbackend;

import com.google.firebase.FirebaseApp;
import com.smartincident.incidentbackend.utils.SpringContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class IncidentBackendApplication implements ApplicationContextAware {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(IncidentBackendApplication.class, args);

        System.out.println("Firebase Apps after startup: " + FirebaseApp.getApps().size());
        FirebaseApp.getApps().forEach(app ->
                System.out.println("Firebase READY → " + app.getName() + " | Project: " + app.getOptions().getProjectId())
        );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContext.setApplicationContext(applicationContext);
    }
}

