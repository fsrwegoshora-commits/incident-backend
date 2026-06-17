package com.smartincident.incidentbackend.enums;

public enum SupportedLanguage {
    EN("en", "English"),
    SW("sw", "Kiswahili");

    private final String code;
    private final String displayName;

    SupportedLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }

    public static SupportedLanguage fromCode(String code) {
        if (code == null) return EN;
        for (SupportedLanguage lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) return lang;
        }
        return EN;
    }
}
