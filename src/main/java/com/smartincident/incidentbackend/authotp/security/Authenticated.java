package com.smartincident.incidentbackend.authotp.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authenticated {
}
