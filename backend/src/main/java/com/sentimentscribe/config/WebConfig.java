package com.sentimentscribe.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowed = resolveAllowedOrigins(corsProperties);
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowed.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    private static List<String> resolveAllowedOrigins(CorsProperties properties) {
        String csv = properties.allowedOriginsCsv();
        if (csv != null && !csv.isBlank()) {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        List<String> allowed = properties.allowedOrigins();
        if (allowed == null) {
            return List.of();
        }
        return allowed.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }
}
