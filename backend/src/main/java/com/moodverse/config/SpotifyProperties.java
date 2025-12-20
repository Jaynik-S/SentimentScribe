package com.moodverse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moodverse.spotify")
public record SpotifyProperties(String clientId, String clientSecret) {
}
