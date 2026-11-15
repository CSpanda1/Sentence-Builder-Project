package edu.utdallas.cs4485.team22.sentencebuilder.model;

import java.time.LocalDateTime;

/**
 * Domain object recording a text file that was imported into the corpus.
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class ImportedFile {

    private final long id;
    private final String fileName;
    private final String filePath;
    private final int wordCount;
    private final int uniqueWordCount;
    private final LocalDateTime importedAt;

    /**
     * Creates an import history record.
     *
     * @param id               surrogate key
     * @param fileName         display name of the file
     * @param filePath         absolute or relative path used at import time
     * @param wordCount        total tokens extracted
     * @param uniqueWordCount  distinct tokens extracted
     * @param importedAt       timestamp of the import
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public ImportedFile(long id, String fileName, String filePath,
                        int wordCount, int uniqueWordCount,
                        LocalDateTime importedAt) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.wordCount = wordCount;
        this.uniqueWordCount = uniqueWordCount;
        this.importedAt = importedAt;
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getWordCount() {
        return wordCount;
    }

    public int getUniqueWordCount() {
        return uniqueWordCount;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }
}
