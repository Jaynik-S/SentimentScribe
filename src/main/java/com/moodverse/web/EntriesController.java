package com.moodverse.web;

import com.moodverse.service.EntryCommand;
import com.moodverse.service.EntryService;
import com.moodverse.service.ServiceResult;
import com.moodverse.usecase.load_entry.LoadEntryOutputData;
import com.moodverse.usecase.save_entry.SaveEntryOutputData;
import com.moodverse.web.dto.DeleteResponse;
import com.moodverse.web.dto.EntryRequest;
import com.moodverse.web.dto.EntryResponse;
import com.moodverse.web.dto.EntrySummaryResponse;
import com.moodverse.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> listEntries() {
        ServiceResult<List<Map<String, Object>>> result = entryService.list();
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
    public ResponseEntity<?> getEntryByPath(@RequestParam("path") String path) {
        ServiceResult<LoadEntryOutputData> result = entryService.load(path);
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(toEntryResponse(result.data()));
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody EntryRequest request) {
        ServiceResult<SaveEntryOutputData> result = entryService.save(toCommand(request));
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toEntryResponse(result.data()));
    }

    @PutMapping
    public ResponseEntity<?> updateEntry(@RequestBody EntryRequest request) {
        ServiceResult<SaveEntryOutputData> result = entryService.save(toCommand(request));
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(toEntryResponse(result.data()));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteEntry(@RequestParam("path") String path) {
        ServiceResult<?> result = entryService.delete(path);
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

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
