package com.sentimentscribe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentimentscribe.tmdb")
public record TmdbProperties(String apiKey) {
}
