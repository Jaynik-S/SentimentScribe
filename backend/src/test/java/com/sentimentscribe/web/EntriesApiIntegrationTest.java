package com.sentimentscribe.web;

import com.sentimentscribe.web.dto.AuthRequest;
import com.sentimentscribe.web.dto.AuthResponse;
import com.sentimentscribe.web.dto.EntryRequest;
import com.sentimentscribe.web.dto.EntryResponse;
import com.sentimentscribe.web.dto.EntrySummaryResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
class EntriesApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15");

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void createLoadAndListEntries() {
        AuthRequest authRequest = new AuthRequest("test-pass");
        ResponseEntity<AuthResponse> authResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/verify", authRequest, AuthResponse.class);
        assertEquals(HttpStatus.OK, authResponse.getStatusCode());

        String text = "a".repeat(60);
        EntryRequest request = new EntryRequest(
                "First Entry",
                text,
                null,
                List.of("focus"),
                LocalDateTime.of(2024, 1, 1, 10, 0)
        );

        ResponseEntity<EntryResponse> createResponse =
                restTemplate.postForEntity(baseUrl() + "/api/entries", request, EntryResponse.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        EntryResponse created = createResponse.getBody();
        assertNotNull(created);
        assertNotNull(created.storagePath());

        String loadUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/api/entries/by-path")
                .queryParam("path", created.storagePath())
                .build()
                .toUriString();
        ResponseEntity<EntryResponse> loadResponse =
                restTemplate.getForEntity(loadUrl, EntryResponse.class);

        assertEquals(HttpStatus.OK, loadResponse.getStatusCode());
        assertNotNull(loadResponse.getBody());
        assertEquals("First Entry", loadResponse.getBody().title());

        ResponseEntity<EntrySummaryResponse[]> listResponse =
                restTemplate.getForEntity(baseUrl() + "/api/entries", EntrySummaryResponse[].class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        EntrySummaryResponse[] summaries = listResponse.getBody();
        assertNotNull(summaries);
        assertTrue(summaries.length >= 1);
    }

    @Test
    void loadMissingEntryReturnsError() {
        String missingPath = "db:missing-entry";
        String url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/api/entries/by-path")
                .queryParam("path", missingPath)
                .build()
                .toUriString();

        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity(url, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}

