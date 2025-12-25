package com.sentimentscribe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentimentscribe.jwt")
public record JwtProperties(String secret, String issuer, long ttlSeconds) {
}
