package edu.utdallas.cs4485.team22.sentencebuilder.model;


/*****************************************************************************************************************
 * GenerationAlgorithm.java
 *
 * Defines the sentence-generation strategies available in the application
 * and provides a display name for each strategy.
 *
 * AI Assistance ChatGPT Codex. Some methods were generated with ChatGPT Codex
 * Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 20th, 2026.
 ******************************************************************************************************************/


/**
 * Algorithms available on the generation screen (WP6 placeholders for the UI).
 *
 *  Written by Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 20th, 2026.
 */
public enum GenerationAlgorithm {

    // Selects a following word with probability based on how often it occurs.
    PROBABILITY_WEIGHTED("Probability-weighted"),
    // Selects the most frequently occurring following word.
    MOST_FREQUENT("Most-frequent follower"),
    // Selects a following word uniformly at random.
    RANDOM_WALK("Random walk");

    // Human-readable label shown for this generation strategy.
    private final String displayName;

    /**
     * Creates a generation strategy with the label shown in the user interface.
     *
     * @param displayName human-readable name for this strategy
     *  Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 20th, 2026.
     */
    GenerationAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable label for this strategy.
     *
     * @return the strategy's display name
     * Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 20th, 2026.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the human-readable label for this strategy.
     *
     * @return the strategy's display name
     *  Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 20th, 2026.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
