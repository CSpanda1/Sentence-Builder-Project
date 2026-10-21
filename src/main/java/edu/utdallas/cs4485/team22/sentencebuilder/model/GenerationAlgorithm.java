package edu.utdallas.cs4485.team22.sentencebuilder.model;

/**
 * Algorithms available on the generation screen (WP6 placeholders for the UI).
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public enum GenerationAlgorithm {

    PROBABILITY_WEIGHTED("Probability-weighted"),
    MOST_FREQUENT("Most-frequent follower"),
    RANDOM_WALK("Random walk");

    private final String displayName;

    GenerationAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
