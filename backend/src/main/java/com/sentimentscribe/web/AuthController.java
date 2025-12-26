package com.sentimentscribe.web;

import com.sentimentscribe.service.AuthService;
import com.sentimentscribe.service.ServiceResult;
import com.sentimentscribe.web.dto.AuthTokenResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import com.sentimentscribe.web.dto.LoginRequest;
import com.sentimentscribe.web.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        ServiceResult<AuthTokenResponse> result =
                authService.register(request.username(), request.password());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(result.data());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        ServiceResult<AuthTokenResponse> result =
                authService.login(request.username(), request.password());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(result.data());
    }

}
