package com.project.shopapp.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

public class ShopAppLocaleResolver extends AcceptHeaderLocaleResolver {
    public static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null || header.isBlank()) {
            return VIETNAMESE;
        }
        try {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(header)) {
                if (range.getWeight() == 0) {
                    continue;
                }
                String language = Locale.forLanguageTag(range.getRange()).getLanguage();
                if (language.equals("vi")) {
                    return VIETNAMESE;
                }
                if (language.equals("en")) {
                    return Locale.ENGLISH;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed language preferences use the application default.
        }
        return VIETNAMESE;
    }
}
