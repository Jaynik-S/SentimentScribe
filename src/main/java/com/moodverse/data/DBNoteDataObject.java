package com.moodverse.data;

import com.moodverse.domain.DiaryEntry;
import org.json.JSONArray;
import com.moodverse.usecase.delete_entry.DeleteEntryUserDataAccessInterface;
import com.moodverse.usecase.load_entry.LoadEntryUserDataAccessInterface;
import com.moodverse.usecase.save_entry.SaveEntryUserDataAccessInterface;
import com.moodverse.usecase.verify_password.RenderEntriesUserDataInterface;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.Map;

public class DBNoteDataObject implements DeleteEntryUserDataAccessInterface, LoadEntryUserDataAccessInterface,
        SaveEntryUserDataAccessInterface, RenderEntriesUserDataInterface {

    public static final Path DEFAULT_BASE_DIR = Paths.get("src/main/java/com/moodverse/data/diary_entry_database");

    private final Path baseDir;

    public DBNoteDataObject() {
        this(DEFAULT_BASE_DIR);
    }

    public DBNoteDataObject(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public DiaryEntry getByPath(String entryPath) throws Exception {
        try {
            EntryRecord data = readEntryRecord(Paths.get(entryPath));
            if (data == null) {
                return null;
            }
            return new DiaryEntry(
                    data.title(),
                    data.text(),
                    data.keywords(),
                    data.storagePath(),
                    data.createdAt(),
                    data.updatedAt()
            );
        }
        catch (Exception error) {
            throw new Exception("Failed to load diary entry from path: " + entryPath, error);
        }
    }

    @Override
    public boolean deleteByPath(String entryPath) {
        if (!existsByPath(entryPath)) {
            return false;
        }
        try {
            Path path = Paths.get(entryPath);
            Files.delete(path);
            return true;
        }
        catch (Exception error) {
            return false;
        }
    }

    @Override
    public boolean save(DiaryEntry entry) throws Exception {
        if (entry == null) {
            throw new Exception("Entry cannot be null.");
        }

        ensureBaseDirExists();

        LocalDateTime createdAt = entry.getCreatedAt() != null ? entry.getCreatedAt() : LocalDateTime.now();
        LocalDateTime updatedAt = entry.getUpdatedAt() != null ? entry.getUpdatedAt() : LocalDateTime.now();

        Path path = resolveSavePath(entry);
        entry.setStoragePath(path.toString());

        JSONObject json = new JSONObject();
        json.put("title", entry.getTitle());
        json.put("text", entry.getText());
        json.put("keywords", entry.getKeywords() == null ? List.of() : entry.getKeywords());
        json.put("created_date", createdAt.toString());
        json.put("updated_date", updatedAt.toString());
        json.put("storage_path", path.toString());

        try {
            Files.writeString(path, json.toString(4));
            return true;
        }
        catch (IOException error) {
            throw new Exception("Failed to save diary entry: " + error.getMessage(), error);
        }
    }

    @Override
    public List<Map<String, Object>> getAll() throws Exception {
        List<Map<String, Object>> allEntries = new ArrayList<>();
        ensureBaseDirExists();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.json")) {
            for (Path filePath : stream) {
                EntryRecord record = readEntryRecord(filePath);
                if (record == null) {
                    continue;
                }
                allEntries.add(record.toMap());
            }
        }
        catch (Exception error) {
            throw new Exception("Failed to load diary entries from base dir: " + baseDir, error);
        }

        return allEntries;
    }

    private boolean existsByPath(String entryPath) {
        Path path = Paths.get(entryPath);
        return Files.exists(path);
    }

    private void ensureBaseDirExists() throws IOException {
        Files.createDirectories(baseDir);
    }

    private Path resolveSavePath(DiaryEntry entry) {
        String storagePath = entry.getStoragePath();
        if (storagePath != null && !storagePath.isBlank()) {
            return Paths.get(storagePath);
        }
        String fileName = nextIndexPrefix() + ") " + sanitizeTitle(entry.getTitle()) + ".json";
        return baseDir.resolve(fileName);
    }

    private int nextIndexPrefix() {
        int max = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.json")) {
            for (Path filePath : stream) {
                String name = filePath.getFileName().toString();
                int end = name.indexOf(')');
                if (end <= 0) {
                    continue;
                }
                try {
                    int prefix = Integer.parseInt(name.substring(0, end).trim());
                    if (prefix > max) {
                        max = prefix;
                    }
                }
                catch (NumberFormatException ignored) {
                }
            }
        }
        catch (IOException ignored) {
        }
        return max + 1;
    }

    private static String sanitizeTitle(String title) {
        String safeTitle = title;
        if (safeTitle == null || safeTitle.isBlank()) {
            safeTitle = "untitled";
        }
        return safeTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private EntryRecord readEntryRecord(Path entryPath) throws Exception {
        if (entryPath == null || !Files.exists(entryPath)) {
            return null;
        }
        try {
            String content = Files.readString(entryPath);
            JSONObject json = new JSONObject(content);

            String title = json.optString("title", "");
            String text = json.optString("text", "");
            List<String> keywords = readKeywords(json.optJSONArray("keywords"));

            LocalDateTime createdAt = parseLocalDateTime(json.optString("created_date", null));
            LocalDateTime updatedAt = parseLocalDateTime(json.optString("updated_date", null));

            String storagePath = json.optString("storage_path", entryPath.toString());

            if (createdAt == null) {
                createdAt = LocalDateTime.now();
            }
            if (updatedAt == null) {
                updatedAt = createdAt;
            }

            return new EntryRecord(title, text, keywords, storagePath, createdAt, updatedAt);
        }
        catch (Exception error) {
            throw new Exception("Failed to parse diary entry from path: " + entryPath, error);
        }
    }

    private static List<String> readKeywords(JSONArray keywordArray) {
        if (keywordArray == null) {
            return List.of();
        }
        return keywordArray.toList().stream()
                .map(Object::toString)
                .toList();
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private record EntryRecord(
            String title,
            String text,
            List<String> keywords,
            String storagePath,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("title", title);
            result.put("text", text);
            result.put("keywords", keywords);
            result.put("createdDate", createdAt);
            result.put("updatedDate", updatedAt);
            result.put("storagePath", storagePath);
            return result;
        }
    }

}

