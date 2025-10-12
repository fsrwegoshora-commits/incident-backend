package com.smartincident.incidentbackend.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Utils {

    public static String generateUniqueID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ----------------- Exception Handling -----------------
    public static String getExceptionMessage(Exception e) {
        String msg = e.getMessage();
        if (e.getCause() != null) {
            Throwable cause = e.getCause();
            while (cause.getCause() != null && cause.getCause() != cause) {
                if (cause.getMessage() != null) msg = cause.getMessage();
                cause = cause.getCause();
            }
            if (cause.getMessage() != null) return cause.getMessage();
        }
        return msg != null ? msg : "";
    }

    // ----------------- String Utilities -----------------
    public static String camelCaseToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        String snake = camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        return snake;
    }

    public static boolean containsString(String[] array, String target) {
        if (array == null || target == null) return false;
        for (String str : array) {
            if (target.equals(str)) return true;
        }
        return false;
    }

    // ----------------- Bean Copy -----------------
    public static void copyProperties(Object source, Object destination) {
        copyProperties(source, destination, null);
    }

    public static void copyProperties(Object source, Object destination, String[] exceptions) {
        BeanUtils.copyProperties(source, destination, getNullPropertyNames(source, exceptions));
    }

    private static String[] getNullPropertyNames(Object source, String[] extra) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        if (extra != null) Collections.addAll(emptyNames, extra);
        return emptyNames.toArray(new String[0]);
    }

    // ----------------- Reflection Entity Instantiation -----------------
    public static <T> T entity(Class<T> clazz, String uid) {
        return createObjectInstance(uid, clazz);
    }

    public static <T> T entity(Class<T> clazz, Long id) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            Field idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(instance, id);
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T createObjectInstance(String uid, Class<T> entityClass) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            Field uidField = entityClass.getDeclaredField("uid");
            uidField.setAccessible(true);
            uidField.set(entity, uid);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ----------------- Hashing -----------------
    public static String harshMethod(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(input.getBytes());
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(Integer.toString((b & 0xFF) + 256, 16).substring(1));
        }
        return sb.toString();
    }

    // ----------------- Financial Year -----------------
    public static String getCurrentFinancialYear() {
        int year = LocalDate.now().getYear();
        int shortYear = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yy")));
        return LocalDate.now().getMonthValue() < 6
                ? (shortYear - 1) + "/" + shortYear
                : shortYear + "/" + (shortYear + 1);
    }

    // ----------------- HTTP / Client Info -----------------
    public static HttpServletRequest getCurrentHttpRequest() {
        return Optional.ofNullable(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .filter(org.springframework.web.context.request.ServletRequestAttributes.class::isInstance)
                .map(org.springframework.web.context.request.ServletRequestAttributes.class::cast)
                .map(org.springframework.web.context.request.ServletRequestAttributes::getRequest)
                .orElse(null);
    }

//    public static ClientRequestInfo getClientRequestData() {
//        return getClientRequestData(getCurrentHttpRequest());
//    }
//
//    public static ClientRequestInfo getClientRequestData(HttpServletRequest request) {
//        if (request == null) return null;
//
//        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
//        String os = detectOS(userAgent);
//        String browser = detectBrowser(userAgent);
//        String remoteAddr = Optional.ofNullable(request.getHeader("X-FORWARDED-FOR"))
//                .orElse(request.getRemoteAddr());
//
//        return new ClientRequestInfo(userAgent, os, browser, remoteAddr);
//    }

    private static String detectOS(String ua) {
        ua = ua.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        else if (ua.contains("mac")) return "Mac";
        else if (ua.contains("linux")) return "Linux";
        else if (ua.contains("android")) return "Android";
        else if (ua.contains("iphone")) return "iPhone";
        else return "Unknown";
    }

    private static String detectBrowser(String ua) {
        ua = ua.toLowerCase();
        if (ua.contains("msie")) return "IE";
        else if (ua.contains("safari") && ua.contains("version")) return "Safari";
        else if (ua.contains("opr") || ua.contains("opera")) return "Opera";
        else if (ua.contains("chrome")) return "Chrome";
        else if (ua.contains("firefox")) return "Firefox";
        else return "Unknown";
    }
}
