package com.moodverse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moodverse.tmdb")
public record TmdbProperties(String apiKey) {
}
