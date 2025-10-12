package com.smartincident.incidentbackend;

import com.smartincident.incidentbackend.utils.SpringContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@SpringBootApplication
public class IncidentBackendApplication implements ApplicationContextAware {

    public static void main(String[] args) {
        SpringApplication.run(IncidentBackendApplication.class, args);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContext.setApplicationContext(applicationContext);
    }
}

