package edu.utdallas.cs4485.team22.sentencebuilder.model;

/*****************************************************************************************************************
 * ImportedFile.java
 *
 * Represents a text file imported into the corpus, including its file details,
 * word counts, and the date and time it was imported.
 *
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import java.time.LocalDateTime;

/**
 * Domain object recording a text file that was imported into the corpus.
 *
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class ImportedFile {

    // Unique identifier for this imported-file record.
    private final long id;
    // Name of the imported file.
    private final String fileName;
    // File path recorded when the file was imported.
    private final String filePath;
    // Total number of words extracted from the file.
    private final int wordCount;
    // Number of distinct words extracted from the file.
    private final int uniqueWordCount;
    // Date and time when the file was imported.
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
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     *
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

    /**
     * Returns the unique identifier for this imported-file record.
     * @return the record identifier
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the name of the imported file.
     * @return the file name
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the path recorded for the imported file.
     * @return the file path
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns the total number of words extracted from the file.
     * @return the total word count
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * Returns the number of distinct words extracted from the file.
     * @return the unique word count
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public int getUniqueWordCount() {
        return uniqueWordCount;
    }

    /**
     * Returns the date and time when the file was imported.
     * @return the import timestamp
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public LocalDateTime getImportedAt() {
        return importedAt;
    }
}
