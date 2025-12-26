package com.sentimentscribe.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimentscribe.web.dto.AuthTokenResponse;
import com.sentimentscribe.web.dto.EntryRequest;
import com.sentimentscribe.web.dto.EntryResponse;
import com.sentimentscribe.web.dto.EntrySummaryResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import com.sentimentscribe.web.dto.LoginRequest;
import com.sentimentscribe.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

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
        registry.add("sentimentscribe.jwt.secret", () -> "test-secret-should-be-at-least-32-bytes-long");
        registry.add("sentimentscribe.jwt.issuer", () -> "sentimentscribe-test");
        registry.add("sentimentscribe.jwt.ttl-seconds", () -> "3600");
    }

    @Test
    void createLoadAndListEntries() {
        AuthTokenResponse authResponse = authForDefaultUser();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.accessToken());

        String titleCiphertext = "Rmlyc3QgRW50cnk=";
        String bodyCiphertext = "Qm9keSBjaXBoZXI=";
        String iv = "AAAAAAAAAAAAAAAAAAAAAA==";
        EntryRequest request = new EntryRequest(
                null,
                LocalDateTime.of(2024, 1, 1, 10, 0),
                titleCiphertext,
                iv,
                bodyCiphertext,
                iv,
                "AES-GCM",
                1
        );

        ResponseEntity<EntryResponse> createResponse =
                restTemplate.exchange(
                        baseUrl() + "/api/entries",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        EntryResponse.class);

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
                restTemplate.exchange(
                        loadUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        EntryResponse.class);

        assertEquals(HttpStatus.OK, loadResponse.getStatusCode());
        assertNotNull(loadResponse.getBody());
        assertEquals(titleCiphertext, loadResponse.getBody().titleCiphertext());
        assertEquals(bodyCiphertext, loadResponse.getBody().bodyCiphertext());

        ResponseEntity<EntrySummaryResponse[]> listResponse =
                restTemplate.exchange(
                        baseUrl() + "/api/entries",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        EntrySummaryResponse[].class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        EntrySummaryResponse[] summaries = listResponse.getBody();
        assertNotNull(summaries);
        assertTrue(summaries.length >= 1);
        boolean found = false;
        for (EntrySummaryResponse summary : summaries) {
            if (titleCiphertext.equals(summary.titleCiphertext())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void loadMissingEntryReturnsError() {
        String missingPath = "db:missing-entry";
        String url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/api/entries/by-path")
                .queryParam("path", missingPath)
                .build()
                .toUriString();

        AuthTokenResponse authResponse = authForDefaultUser();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.accessToken());
        ResponseEntity<ErrorResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    @Test
    void missingTokenReturnsUnauthorized() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity(baseUrl() + "/api/entries", ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    @Test
    void listEntriesOmitsPlaintextFields() throws Exception {
        AuthTokenResponse authResponse = authForDefaultUser();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.accessToken());

        EntryRequest request = new EntryRequest(
                null,
                LocalDateTime.of(2024, 1, 2, 9, 0),
                "VGVzdCBUaXRsZQ==",
                "AAAAAAAAAAAAAAAAAAAAAA==",
                "VGVzdCBCb2R5",
                "AAAAAAAAAAAAAAAAAAAAAA==",
                "AES-GCM",
                1
        );

        ResponseEntity<EntryResponse> createResponse =
                restTemplate.exchange(
                        baseUrl() + "/api/entries",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        EntryResponse.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        ResponseEntity<String> listResponse =
                restTemplate.exchange(
                        baseUrl() + "/api/entries",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);

        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertNotNull(listResponse.getBody());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(listResponse.getBody());
        assertTrue(root.isArray());
        assertTrue(root.size() > 0);

        JsonNode first = root.get(0);
        assertFalse(first.has("title"));
        assertFalse(first.has("text"));
        assertFalse(first.has("keywords"));
    }

    private AuthTokenResponse authForDefaultUser() {
        RegisterRequest authRequest = new RegisterRequest("default", "test-pass");
        ResponseEntity<AuthTokenResponse> registerResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/register", authRequest, AuthTokenResponse.class);
        if (registerResponse.getStatusCode() == HttpStatus.OK && registerResponse.getBody() != null) {
            return registerResponse.getBody();
        }
        LoginRequest loginRequest = new LoginRequest("default", "test-pass");
        ResponseEntity<AuthTokenResponse> loginResponse =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", loginRequest, AuthTokenResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        return loginResponse.getBody();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}

