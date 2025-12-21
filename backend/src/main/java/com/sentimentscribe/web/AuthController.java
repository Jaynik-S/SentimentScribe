package com.sentimentscribe.web;

import com.sentimentscribe.service.AuthService;
import com.sentimentscribe.service.ServiceResult;
import com.sentimentscribe.usecase.verify_password.VerifyPasswordOutputData;
import com.sentimentscribe.web.dto.AuthRequest;
import com.sentimentscribe.web.dto.AuthResponse;
import com.sentimentscribe.web.dto.EntrySummaryResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPassword(@RequestBody AuthRequest request) {
        ServiceResult<VerifyPasswordOutputData> result = authService.verifyPassword(request.password());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        VerifyPasswordOutputData data = result.data();
        List<EntrySummaryResponse> entries = data.getAllEntries()
                .stream()
                .map(AuthController::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(new AuthResponse(data.passwordStatus(), entries));
    }

    private static EntrySummaryResponse toSummaryResponse(Map<String, Object> entry) {
        return new EntrySummaryResponse(
                stringValue(entry.get("title")),
                stringValue(entry.get("storagePath")),
                asLocalDateTime(entry.get("createdDate")),
                asLocalDateTime(entry.get("updatedDate")),
                asStringList(entry.get("keywords"))
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return null;
    }

    private static List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
