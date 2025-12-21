package com.sentimentscribe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentimentscribe.spotify")
public record SpotifyProperties(String clientId, String clientSecret) {
}
