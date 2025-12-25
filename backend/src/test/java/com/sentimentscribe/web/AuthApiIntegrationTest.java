package com.sentimentscribe.web;

import com.sentimentscribe.web.dto.AuthTokenResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import com.sentimentscribe.web.dto.LoginRequest;
import com.sentimentscribe.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
class AuthApiIntegrationTest {

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
    void registerReturnsTokenAndE2eeParams() {
        RegisterRequest request = new RegisterRequest("register-user", "test-pass");
        ResponseEntity<AuthTokenResponse> response =
                restTemplate.postForEntity(baseUrl() + "/api/auth/register", request, AuthTokenResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthTokenResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.accessToken());
        assertEquals("Bearer", body.tokenType());
        assertTrue(body.expiresIn() > 0);
        assertNotNull(body.user());
        assertNotNull(body.user().id());
        assertEquals("register-user", body.user().username());
        assertNotNull(body.e2ee());
        assertEquals("PBKDF2-SHA256", body.e2ee().kdf());
        assertNotNull(body.e2ee().salt());
        assertFalse(body.e2ee().salt().isBlank());
        assertTrue(body.e2ee().iterations() > 0);
    }

    @Test
    void loginReturnsToken() {
        RegisterRequest request = new RegisterRequest("login-user", "test-pass");
        restTemplate.postForEntity(baseUrl() + "/api/auth/register", request, AuthTokenResponse.class);

        LoginRequest login = new LoginRequest("login-user", "test-pass");
        ResponseEntity<AuthTokenResponse> response =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, AuthTokenResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
    }

    @Test
    void loginWithWrongPasswordReturnsBadRequest() {
        RegisterRequest request = new RegisterRequest("wrong-pass-user", "test-pass");
        restTemplate.postForEntity(baseUrl() + "/api/auth/register", request, AuthTokenResponse.class);

        LoginRequest login = new LoginRequest("wrong-pass-user", "wrong");
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity(baseUrl() + "/api/auth/login", login, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().error());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
