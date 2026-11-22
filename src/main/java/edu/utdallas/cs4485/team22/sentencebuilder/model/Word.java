package edu.utdallas.cs4485.team22.sentencebuilder.model;

/*****************************************************************************************************************
 * Word.java
 *
 * Represents a normalized word in the text corpus, including its frequency
 * and whether it appears at sentence boundaries.
 *
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

/**
 * Domain object representing a word stored in the corpus.
 *
 * Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class Word {

    // Unique identifier for this word record.
    private long id;
    // Normalized text of the word.
    private String text;
    // Number of times this word appears in the corpus.
    private int frequency;
    // Whether this word appears at the beginning of a sentence.
    private boolean sentenceStart;
    // Whether this word appears at the end of a sentence.
    private boolean sentenceEnd;

    /**
     * Creates a word with the given attributes.
     *
     * @param id             surrogate key
     * @param text           word text (normalized)
     * @param frequency      observed count in the corpus
     * @param sentenceStart  true if this word begins at least one sentence
     * @param sentenceEnd    true if this word ends at least one sentence
     *
     *  Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     *
     */
    public Word(long id, String text, int frequency,
                boolean sentenceStart, boolean sentenceEnd) {
        this.id = id;
        this.text = text;
        this.frequency = frequency;
        this.sentenceStart = sentenceStart;
        this.sentenceEnd = sentenceEnd;
    }

    /**
     * Returns the database identifier for this word.
     * This getter allows other classes to read the private identifier.
     *
     * @return the word's identifier
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     *
     */
    public long getId() {
        return id;
    }

    /**
     * Updates the database identifier for this word.
     * This setter is used when the identifier needs to be assigned or changed.
     *
     * @param id the new word identifier
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     *
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns this word's normalized text.
     * This getter lets other classes use the word without accessing the field directly.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     *
     * @return the normalized word text
     */
    public String getText() {
        return text;
    }

    /**
     * Updates this word's text.
     * This setter is used when the word text needs to be changed.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026/
     *
     * @param text the new word text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns how many times this word occurs in the corpus.
     * This getter lets other classes use its frequency for display or processing.
     *
     * @return the word's occurrence count
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * Updates how many times this word occurs in the corpus.
     * This setter is used when corpus counts are loaded or recalculated.
     *
     * @param frequency the new occurrence count
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * Reports whether this word appears at the start of a sentence.
     * This getter lets sentence-generation logic identify valid starting words.
     *
     * @return {@code true} if the word starts a sentence
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public boolean isSentenceStart() {
        return sentenceStart;
    }

    /**
     * Sets whether this word appears at the start of a sentence.
     * This setter is used when sentence-boundary information is recorded or updated.
     *
     * @param sentenceStart {@code true} if the word starts a sentence
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void setSentenceStart(boolean sentenceStart) {
        this.sentenceStart = sentenceStart;
    }

    /**
     * Reports whether this word appears at the end of a sentence.
     * This getter lets sentence-generation logic identify valid ending words.
     *
     * @return {@code true} if the word ends a sentence
     *
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public boolean isSentenceEnd() {
        return sentenceEnd;
    }

    /**
     * Sets whether this word appears at the end of a sentence.
     * This setter is used when sentence-boundary information is recorded or updated.
     *
     * @param sentenceEnd {@code true} if the word ends a sentence
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void setSentenceEnd(boolean sentenceEnd) {
        this.sentenceEnd = sentenceEnd;
    }

    /**
     * Returns the word text as this object's string representation.
     * This is useful when the object is displayed or converted to text.
     *
     * @return the word text
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public String toString() {
        return text;
    }
}
