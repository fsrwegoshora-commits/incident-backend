package com.smartincident.incidentbackend.i18n;

import com.smartincident.incidentbackend.enums.SupportedLanguage;
import com.smartincident.incidentbackend.utils.LoggedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Central service for resolving localized messages.
 *
 * Usage:
 *   localizationService.msg("notification.incident.resolved.title")
 *   localizationService.msg("notification.incident.resolved.message", lang, "INC-001")
 *   localizationService.msgForUser("incident.status.RESOLVED", userPreferredLang)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizationService {

    private final MessageSource messageSource;

    /** Resolve a message in the currently-authenticated user's preferred language. */
    public String msg(String key, Object... args) {
        return msgForLang(key, LoggedUser.getPreferredLanguage(), args);
    }

    /** Resolve a message in the specified language code ("en", "sw", …). */
    public String msgForLang(String key, String langCode, Object... args) {
        Locale locale = resolveLocale(langCode);
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (Exception e) {
            log.trace("Missing i18n key '{}' for locale '{}': {}", key, locale, e.getMessage());
            // Fall back to English, then the bare key
            try {
                return messageSource.getMessage(key, args, Locale.ENGLISH);
            } catch (Exception ex) {
                return key;
            }
        }
    }

    /**
     * Resolve an incident status label for a given language.
     * e.g. statusLabel("RESOLVED", "sw") → "Imetatuliwa"
     */
    public String statusLabel(String statusName, String langCode) {
        return msgForLang("incident.status." + statusName, langCode);
    }

    /**
     * Resolve an incident type label.
     * e.g. typeLabel("FIRE", "sw") → "Moto"
     */
    public String typeLabel(String typeName, String langCode) {
        return msgForLang("incident.type." + typeName, langCode);
    }

    /**
     * Resolve an emergency level label.
     * e.g. levelLabel("CRITICAL", "sw") → "Hatari Sana"
     */
    public String levelLabel(String levelName, String langCode) {
        return msgForLang("emergency.level." + levelName, langCode);
    }

    /** Resolve a role display name. */
    public String roleLabel(String roleName, String langCode) {
        return msgForLang("role." + roleName, langCode);
    }

    /** Build a localized push notification title + body pair for the given user's language. */
    public LocalizedMessage buildMessage(String titleKey, String bodyKey, String langCode, Object... args) {
        return new LocalizedMessage(
                msgForLang(titleKey, langCode),
                msgForLang(bodyKey, langCode, args)
        );
    }

    private Locale resolveLocale(String langCode) {
        SupportedLanguage lang = SupportedLanguage.fromCode(langCode);
        return switch (lang) {
            case SW -> new Locale("sw");
            default -> Locale.ENGLISH;
        };
    }

    public record LocalizedMessage(String title, String body) {}
}
