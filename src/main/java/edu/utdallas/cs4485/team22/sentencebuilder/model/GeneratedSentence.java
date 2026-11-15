package edu.utdallas.cs4485.team22.sentencebuilder.model;

import java.time.LocalDateTime;

/**
 * Domain object for a sentence produced by a generation algorithm.
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class GeneratedSentence {

    private final long id;
    private final String text;
    private final String algorithm;
    private final String startWord;
    private final LocalDateTime createdAt;
    private final boolean duplicate;

    /**
     * Creates a generated-sentence record.
     *
     * @param id         surrogate key
     * @param text       full sentence text
     * @param algorithm  name of the algorithm that produced it
     * @param startWord  seed word supplied by the user
     * @param createdAt  when it was generated
     * @param duplicate  true if an identical sentence already exists
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public GeneratedSentence(long id, String text, String algorithm,
                             String startWord, LocalDateTime createdAt,
                             boolean duplicate) {
        this.id = id;
        this.text = text;
        this.algorithm = algorithm;
        this.startWord = startWord;
        this.createdAt = createdAt;
        this.duplicate = duplicate;
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getStartWord() {
        return startWord;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    @Override
    public String toString() {
        return text;
    }
}
