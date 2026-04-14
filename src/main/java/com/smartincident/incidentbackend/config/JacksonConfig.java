package com.smartincident.incidentbackend.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Registers the Hibernate 6 module with Jackson so that lazy-loaded
     * entity proxies (byte-buddy) are serialized correctly instead of being
     * silently dropped or throwing an exception.
     *
     * FORCE_LAZY_LOADING: initializes uninitialized proxies rather than
     * serializing them as null, so every lazy association that is reachable
     * within an open session is included in the JSON response.
     */
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
