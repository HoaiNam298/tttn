package com.project.shopapp.configuration;

import com.project.shopapp.components.I18nMessages;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import(I18nMessages.class)
public class I18nConfig implements WebMvcConfigurer {
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(ShopAppLocaleResolver.VIETNAMESE);
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Bean
    public ShopAppLocaleResolver localeResolver() {
        return new ShopAppLocaleResolver();
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.setConfigurationInitializer(
                configuration ->
                        configuration.clockProvider(
                                () ->
                                        java.time.Clock.system(
                                                java.time.ZoneId.of("Asia/Ho_Chi_Minh"))));
        return validator;
    }

    @Override
    public org.springframework.validation.Validator getValidator() {
        return validator(messageSource());
    }
}
