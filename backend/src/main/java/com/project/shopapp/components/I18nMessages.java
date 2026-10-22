package com.project.shopapp.components;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class I18nMessages {
    private final MessageSource source;

    public I18nMessages(MessageSource source) {
        this.source = source;
    }

    public String get(String key, Object... arguments) {
        return getForLocale(LocaleContextHolder.getLocale(), key, arguments);
    }

    public String getForLocale(Locale locale, String key, Object... arguments) {
        try {
            return source.getMessage(key, arguments, locale);
        } catch (NoSuchMessageException exception) {
            log.warn("Missing translation key={} locale={}", key, locale);
            return source.getMessage(MessageKeys.INTERNAL_ERROR, null, locale);
        }
    }
}
