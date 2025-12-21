package com.sentimentscribe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentimentscribe.auth")
public record AuthProperties(String password) {
}
