package com.sentimentscribe.web;

import com.sentimentscribe.domain.MovieRecommendation;
import com.sentimentscribe.domain.SongRecommendation;
import com.sentimentscribe.service.AnalysisService;
import com.sentimentscribe.service.RecommendationService;
import com.sentimentscribe.service.ServiceResult;
import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsOutputData;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsOutputData;
import com.sentimentscribe.web.dto.AnalysisRequest;
import com.sentimentscribe.web.dto.AnalysisResponse;
import com.sentimentscribe.web.dto.AuthTokenResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import com.sentimentscribe.web.dto.LoginRequest;
import com.sentimentscribe.web.dto.RecommendationRequest;
import com.sentimentscribe.web.dto.RecommendationResponse;
import com.sentimentscribe.web.dto.RegisterRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
class AnalysisRecommendationsApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15");

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @MockBean
    private AnalysisService analysisService;

    @MockBean
    private RecommendationService recommendationService;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("sentimentscribe.jwt.secret", () -> "test-secret-should-be-at-least-32-bytes-long");
        registry.add("sentimentscribe.jwt.issuer", () -> "sentimentscribe-test");
        registry.add("sentimentscribe.jwt.ttl-seconds", () -> "3600");
    }

    @BeforeEach
    void setupMocks() {
        when(analysisService.analyze(anyString()))
                .thenReturn(ServiceResult.success(new AnalyzeKeywordsOutputData(List.of("focus"))));

        GetRecommendationsOutputData outputData = new GetRecommendationsOutputData(
                List.of("focus"),
                List.of(new SongRecommendation("2024", "img", "song", "artist", "90", "url")),
                List.of(new MovieRecommendation("2023", "img2", "movie", "8", "overview"))
        );
        when(recommendationService.recommend(anyString()))
                .thenReturn(ServiceResult.success(outputData));
    }

    @Test
    void analysisRequiresToken() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity(
                        baseUrl() + "/api/analysis",
                        new AnalysisRequest("Hello"),
                        ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    @Test
    void recommendationsRequireToken() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity(
                        baseUrl() + "/api/recommendations",
                        new RecommendationRequest("Hello"),
                        ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    @Test
    void analysisWithTokenReturnsOk() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authForDefaultUser().accessToken());
        ResponseEntity<AnalysisResponse> response =
                restTemplate.exchange(
                        baseUrl() + "/api/analysis",
                        HttpMethod.POST,
                        new HttpEntity<>(new AnalysisRequest("Hello"), headers),
                        AnalysisResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of("focus"), response.getBody().keywords());
    }

    @Test
    void recommendationsWithTokenReturnOk() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authForDefaultUser().accessToken());
        ResponseEntity<RecommendationResponse> response =
                restTemplate.exchange(
                        baseUrl() + "/api/recommendations",
                        HttpMethod.POST,
                        new HttpEntity<>(new RecommendationRequest("Hello"), headers),
                        RecommendationResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of("focus"), response.getBody().keywords());
        assertEquals(1, response.getBody().songs().size());
        assertEquals(1, response.getBody().movies().size());
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
