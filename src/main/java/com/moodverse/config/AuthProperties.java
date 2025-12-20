package com.moodverse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moodverse.auth")
public record AuthProperties(String password) {
}
