package edu.utdallas.cs4485.team22.sentencebuilder.model;

/**
 * Domain object representing a word stored in the corpus.
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class Word {

    private long id;
    private String text;
    private int frequency;
    private boolean sentenceStart;
    private boolean sentenceEnd;

    /**
     * Creates a word with the given attributes.
     *
     * @param id             surrogate key
     * @param text           word text (normalized)
     * @param frequency      observed count in the corpus
     * @param sentenceStart  true if this word begins at least one sentence
     * @param sentenceEnd    true if this word ends at least one sentence
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public Word(long id, String text, int frequency,
                boolean sentenceStart, boolean sentenceEnd) {
        this.id = id;
        this.text = text;
        this.frequency = frequency;
        this.sentenceStart = sentenceStart;
        this.sentenceEnd = sentenceEnd;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public boolean isSentenceStart() {
        return sentenceStart;
    }

    public void setSentenceStart(boolean sentenceStart) {
        this.sentenceStart = sentenceStart;
    }

    public boolean isSentenceEnd() {
        return sentenceEnd;
    }

    public void setSentenceEnd(boolean sentenceEnd) {
        this.sentenceEnd = sentenceEnd;
    }

    @Override
    public String toString() {
        return text;
    }
}
