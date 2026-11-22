package edu.utdallas.cs4485.team22.sentencebuilder.model;

/*****************************************************************************************************************
 * GeneratedSentence.java
 *
 * Represents a sentence created by the application, including its text,
 * generation algorithm, starting word, creation time, and duplicate status.
 *
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/


import java.time.LocalDateTime;

/**
 * Domain object for a sentence produced by a generation algorithm.
 *
 * Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
 */
public class GeneratedSentence {
    // Unique identifier for this generated-sentence record.
    private final long id;
    // Full text of the generated sentence.
    private final String text;
    // Name of the generation algorithm used to create the sentence.
    private final String algorithm;
    // Word used to begin generating the sentence.
    private final String startWord;
    // Date and time when the sentence was generated.
    private final LocalDateTime createdAt;
    // Whether this sentence duplicates a previously generated sentence.
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
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public GeneratedSentence(long id, String text, String algorithm,
                             String startWord, LocalDateTime createdAt,
                             boolean duplicate) {
        // Store the unique identifier for this sentence record.
        this.id = id;
        // Store the generated sentence text.
        this.text = text;
        // Store the algorithm that produced the sentence.
        this.algorithm = algorithm;
        // Store the word used to start sentence generation.
        this.startWord = startWord;
        // Store when the sentence was created.
        this.createdAt = createdAt;
        // Store whether the sentence duplicates an existing one.
        this.duplicate = duplicate;
    }

    /**
     * Returns this generated sentence record's unique identifier.
     * @return the record identifier
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public long getId()
    {
        return id;
    }

    /**
     * Returns the generated sentence text.
     * @return the sentence text
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getText() {

        return text;
    }

    /**
     * Returns the name of the algorithm that generated this sentence.
     * @return the generation algorithm name
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the word used to start generating this sentence.
     * @return the starting word
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getStartWord() {
        return startWord;
    }

    /**
     * Returns the date and time when this sentence was generated.
     * @return the creation date and time
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Reports whether this sentence duplicates a previously generated sentence.
     * @return {@code true} if it is a duplicate; otherwise {@code false}
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public boolean isDuplicate() {
        return duplicate;
    }

    /**
     * Returns the generated sentence text as this object's string representation.
     * @return the sentence text
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    @Override
    public String toString() {
        return text;
    }
}
