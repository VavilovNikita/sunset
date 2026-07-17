package com.sunsetbeach.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonNullableModuleCustomizer() {
        return builder -> builder.addModule(new JsonNullableJackson3Module());
    }
}
