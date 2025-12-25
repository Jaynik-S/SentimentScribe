package com.sentimentscribe.web;

import com.sentimentscribe.service.EntryCommand;
import com.sentimentscribe.service.EntryService;
import com.sentimentscribe.service.ServiceResult;
import com.sentimentscribe.usecase.load_entry.LoadEntryOutputData;
import com.sentimentscribe.usecase.save_entry.SaveEntryOutputData;
import com.sentimentscribe.web.dto.DeleteResponse;
import com.sentimentscribe.web.dto.EntryRequest;
import com.sentimentscribe.web.dto.EntryResponse;
import com.sentimentscribe.web.dto.EntrySummaryResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entries")
public class EntriesController {

    private final EntryService entryService;

    public EntriesController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping
    public ResponseEntity<?> listEntries(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = requireUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
        }
        ServiceResult<List<Map<String, Object>>> result = entryService.list(userId);
        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(result.errorMessage()));
        }
        List<EntrySummaryResponse> summaries = result.data().stream()
                .map(EntriesController::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/by-path")
    public ResponseEntity<?> getEntryByPath(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam("path") String path) {
        UUID userId = requireUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
        }
        ServiceResult<LoadEntryOutputData> result = entryService.load(userId, path);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(toEntryResponse(result.data()));
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@AuthenticationPrincipal Jwt jwt,
                                         @RequestBody EntryRequest request) {
        UUID userId = requireUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
        }
        ServiceResult<SaveEntryOutputData> result = entryService.save(userId, toCommand(request));
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toEntryResponse(result.data()));
    }

    @PutMapping
    public ResponseEntity<?> updateEntry(@AuthenticationPrincipal Jwt jwt,
                                         @RequestBody EntryRequest request) {
        UUID userId = requireUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
        }
        ServiceResult<SaveEntryOutputData> result = entryService.save(userId, toCommand(request));
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(toEntryResponse(result.data()));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteEntry(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam("path") String path) {
        UUID userId = requireUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized"));
        }
        ServiceResult<?> result = entryService.delete(userId, path);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(new DeleteResponse(true, path));
    }

    private static EntryCommand toCommand(EntryRequest request) {
        return new EntryCommand(
                request.title(),
                request.text(),
                request.storagePath(),
                request.keywords(),
                request.createdAt()
        );
    }

    private static EntryResponse toEntryResponse(SaveEntryOutputData data) {
        return new EntryResponse(
                data.getTitle(),
                data.getText(),
                data.getStoragePath(),
                data.getDate(),
                null,
                data.getKeywords()
        );
    }

    private static EntryResponse toEntryResponse(LoadEntryOutputData data) {
        return new EntryResponse(
                data.getTitle(),
                data.getText(),
                data.getStoragePath(),
                data.getDate(),
                null,
                data.getKeywords()
        );
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

    private static UUID requireUserId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object claim = jwt.getClaim("uid");
        if (claim instanceof UUID uuid) {
            return uuid;
        }
        if (claim instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
