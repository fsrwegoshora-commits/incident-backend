package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.enums.Permission;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    Permission[] value();
    /** true = caller must have ALL listed permissions; false (default) = ANY one suffices. */
    boolean requireAll() default false;
}
