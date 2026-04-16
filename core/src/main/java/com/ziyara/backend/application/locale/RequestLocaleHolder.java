package com.ziyara.backend.application.locale;

import java.util.Locale;

/**
 * Holds the current request locale so services can return localized content (_ar vs default).
 * Set by LocaleFilter from Accept-Language header.
 */
public final class RequestLocaleHolder {

    private static final ThreadLocal<Locale> HOLDER = new ThreadLocal<>();

    public static void setLocale(Locale locale) {
        HOLDER.set(locale);
    }

    public static Locale getLocale() {
        Locale l = HOLDER.get();
        return l != null ? l : Locale.ENGLISH;
    }

    /** True when current request prefers Arabic (ar or ar-*). */
    public static boolean isArabic() {
        String lang = getLocale().getLanguage();
        return "ar".equalsIgnoreCase(lang);
    }

    /** Returns localized string: Arabic if locale is ar and arValue is non-null, else default. */
    public static String localized(String defaultValue, String arValue) {
        if (isArabic() && arValue != null && !arValue.isBlank()) {
            return arValue;
        }
        return defaultValue != null ? defaultValue : "";
    }

    public static void clear() {
        HOLDER.remove();
    }
}
