package com.moodverse.web;

import com.moodverse.data.DBNoteDataObject;
import com.moodverse.data.DiaryEntryRepository;
import com.moodverse.web.dto.EntryRequest;
import com.moodverse.web.dto.EntryResponse;
import com.moodverse.web.dto.EntrySummaryResponse;
import com.moodverse.web.dto.ErrorResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(EntriesApiIntegrationTest.TestConfig.class)
class EntriesApiIntegrationTest {

    private static final Path BASE_DIR = createTempDir();

    @Autowired
    private TestRestTemplate restTemplate;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        DiaryEntryRepository diaryEntryRepository() {
            return new DBNoteDataObject(BASE_DIR);
        }
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (Files.exists(BASE_DIR)) {
            Files.walk(BASE_DIR)
                    .sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    void createLoadAndListEntries() {
        String text = "a".repeat(60);
        EntryRequest request = new EntryRequest(
                "First Entry",
                text,
                null,
                List.of("focus"),
                LocalDateTime.of(2024, 1, 1, 10, 0)
        );

        ResponseEntity<EntryResponse> createResponse =
                restTemplate.postForEntity("/api/entries", request, EntryResponse.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        EntryResponse created = createResponse.getBody();
        assertNotNull(created);
        assertNotNull(created.storagePath());

        String loadUrl = UriComponentsBuilder.fromPath("/api/entries/by-path")
                .queryParam("path", created.storagePath())
                .build()
                .toUriString();
        ResponseEntity<EntryResponse> loadResponse =
                restTemplate.getForEntity(loadUrl, EntryResponse.class);

        assertEquals(HttpStatus.OK, loadResponse.getStatusCode());
        assertNotNull(loadResponse.getBody());
        assertEquals("First Entry", loadResponse.getBody().title());

        ResponseEntity<EntrySummaryResponse[]> listResponse =
                restTemplate.getForEntity("/api/entries", EntrySummaryResponse[].class);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        EntrySummaryResponse[] summaries = listResponse.getBody();
        assertNotNull(summaries);
        assertTrue(summaries.length >= 1);
    }

    @Test
    void loadMissingEntryReturnsError() {
        String missingPath = BASE_DIR.resolve("missing-entry.json").toString();
        String url = UriComponentsBuilder.fromPath("/api/entries/by-path")
                .queryParam("path", missingPath)
                .build()
                .toUriString();

        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity(url, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("moodverse-entry-test-");
        } catch (IOException error) {
            throw new IllegalStateException("Failed to create temp directory", error);
        }
    }
}
