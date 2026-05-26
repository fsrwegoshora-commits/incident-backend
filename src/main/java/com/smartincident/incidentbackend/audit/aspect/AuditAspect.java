package com.smartincident.incidentbackend.audit.aspect;

import com.smartincident.incidentbackend.audit.service.AuditLogService;
import com.smartincident.incidentbackend.utils.LoggedUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    // Intercept successful writes on all @RestController classes
    @AfterReturning(
        pointcut = "within(@org.springframework.web.bind.annotation.RestController *) && " +
                   "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
                   " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
                   " @annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
                   " @annotation(org.springframework.web.bind.annotation.DeleteMapping))",
        returning = "result"
    )
    public void onWriteSuccess(JoinPoint jp, Object result) {
        captureAudit(jp, result, true);
    }

    // Also capture failures so operators know something went wrong
    @AfterThrowing(
        pointcut = "within(@org.springframework.web.bind.annotation.RestController *) && " +
                   "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
                   " @annotation(org.springframework.web.bind.annotation.PutMapping) || " +
                   " @annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
                   " @annotation(org.springframework.web.bind.annotation.DeleteMapping))",
        throwing = "ex"
    )
    public void onWriteFailure(JoinPoint jp, Exception ex) {
        captureAudit(jp, null, false);
    }

    private void captureAudit(JoinPoint jp, Object result, boolean success) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();

            String method  = req.getMethod();
            String uri     = req.getRequestURI();
            String ip      = resolveClientIp(req);

            String action  = httpMethodToAction(method);
            String entityType = jp.getTarget().getClass()
                    .getSimpleName().replace("Controller", "");
            String entityUid  = extractEntityUid(result);
            String methodName = jp.getSignature().getName();

            auditLogService.log(
                    LoggedUser.getUid(),
                    LoggedUser.getName(),
                    roleString(),
                    action, entityType, entityUid,
                    uri, method, toReadable(methodName), ip, success);

        } catch (Exception e) {
            log.debug("AuditAspect capture failed: {}", e.getMessage());
        }
    }

    private static String httpMethodToAction(String httpMethod) {
        return switch (httpMethod.toUpperCase()) {
            case "POST"  -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default      -> httpMethod.toUpperCase();
        };
    }

    private static String roleString() {
        try {
            var role = LoggedUser.getRole();
            return role != null ? role.name() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Try to pull the uid from Response<T>.getData().getUid() */
    private static String extractEntityUid(Object result) {
        if (result == null) return null;
        try {
            Method getData = result.getClass().getMethod("getData");
            Object data = getData.invoke(result);
            if (data == null) return null;
            Method getUid = data.getClass().getMethod("getUid");
            Object uid = getUid.invoke(data);
            return uid != null ? uid.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static String toReadable(String methodName) {
        if (methodName == null || methodName.isEmpty()) return "";
        String spaced = methodName.replaceAll("([A-Z])", " $1").trim();
        if (spaced.isEmpty()) return methodName;
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
