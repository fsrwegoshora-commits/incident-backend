package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.enums.Role;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizedRole {
    Role[] value(); // roles allowed to access
}

