package com.sunsetbeach.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

@Configuration
public class WebConfig {

    /**
     * Zod's validation messages are always English regardless of the client's locale - pin
     * Bean Validation's message interpolation (which otherwise follows the server/request
     * locale) to en-US so it doesn't drift by deployment environment.
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.US);
    }
}
