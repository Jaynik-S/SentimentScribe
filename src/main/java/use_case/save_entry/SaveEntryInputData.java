package use_case.save_entry;

import java.util.List;
import java.time.LocalDateTime;

public class SaveEntryInputData {
    private final String title;
    private final LocalDateTime date;
    private final String textBody;
    private final String storagePath;
    private final List<String> keywords;

    public SaveEntryInputData(String title,
                              LocalDateTime date,
                              String textBody,
                              String storagePath,
                              List<String> keywords) {
        this.title = title;
        this.date = date;
        this.textBody = textBody;
        this.storagePath = storagePath;
        this.keywords = keywords;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public List<String> getKeywords() {
        return keywords;
    }

}
