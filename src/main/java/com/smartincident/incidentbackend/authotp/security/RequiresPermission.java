package com.smartincident.incidentbackend.authotp.security;

import com.smartincident.incidentbackend.enums.Permission;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    Permission[] value();
    /**
     * true (default) = caller must have ALL listed permissions; false = ANY one suffices.
     * Defaults to fail-closed (AND) so a future multi-permission usage that means "needs both"
     * doesn't silently grant access via OR semantics. Has no effect on single-permission usages.
     */
    boolean requireAll() default true;
}
