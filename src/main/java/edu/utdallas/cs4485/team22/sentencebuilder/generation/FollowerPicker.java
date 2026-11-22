package edu.utdallas.cs4485.team22.sentencebuilder.generation;

/*****************************************************************************************************************
 * FollowerPicker.java
 *
 * Selects the next word in a generated sentence from observed word pairs,
 * using the chosen strategy or a random fallback when no pair is available.
 *
 * AI Assistance Claude AI. Some methods were generated with Claude
 * Written by: Umair Malik(UAM200000) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Chooses the next word from follower pairs using a generation algorithm.
 *
 * Written by: Umair Malik(UAM200000) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 * Prompt to Claude A: Help generate a class that picks the next word in a generated sentence from observed word pairs, using the chosen strategy or a random fallback when no pair is available. The class should have a method to check if a given text is a single word (non-whitespace). It should also have a constructor that takes a shared random source for selecting words randomly. The main method should pick a following word according to the algorithm, and if no pairs exist, it should choose from fallback words. The class should be final and use appropriate imports.
 */
public final class FollowerPicker {

    // A regex pattern that matches a single word (non-whitespace characters).
    private static final Pattern SINGLE_WORD = Pattern.compile("(?U)\\S+");

    // A shared random source for selecting words randomly.
    private final Random random;

    /**
     * Checks if the given text is a single word (non-whitespace).
     *
     * @param text the text to check
     * @return true if the text is a single word, false otherwise
     * Method created by Claude AI
     * Prompt: Create a method that checks if a given text is a single word (non-whitespace). The method should take a string as input and return a boolean indicating whether the string matches the SINGLE_WORD regex pattern. The method should be static and public.
     * Written by: Umair Malik(UAM200000) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public static boolean isSingleWord(String text) {

        // Use the SINGLE_WORD regex pattern to check if the text matches a single word.
        return SINGLE_WORD.matcher(text).matches();
    }

    /**
     * @param random shared random source (seeded by caller for tests)
     * Method created by Claude AI
     * Prompt: Create a constructor for the FollowerPicker class that takes a shared random source as a parameter. The constructor should initialize the random field with the provided random source. The constructor should be public and take a Random object as input.
     * Written by: Umair Malik(UAM200000) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public FollowerPicker(Random random) {
        this.random = random;
    }

    /**
     * Picks a following word according to the algorithm.
     *
     * @param pairs     follower pairs for the current word (may be empty)
     * @param algorithm generation strategy
     * @param fallback  words to choose from if no pairs exist
     * @return next word text, if any
     * Method created by Claude AI
     * Prompt: Create a method that picks a following word according to the algorithm. The method should take three parameters: a list of WordPair objects, a GenerationAlgorithm object, and a list of fallback words. The method should return an Optional<String> representing the next word text, if any. The method should use a switch statement to handle different generation strategies (MOST_FREQUENT, PROBABILITY_WEIGHTED, and RANDOM_WALK) and return the appropriate word based on the strategy. If no pairs exist, the method should choose from fallback words. The method should be public and return an Optional<String>.
     * Written by: Umair Malik(UAM200000) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public Optional<String> pick(List<WordPair> pairs, GenerationAlgorithm algorithm,
                                 List<String> fallback) {

        // Use the available follower pairs when the list exists and contains entries.
        if (pairs != null && !pairs.isEmpty()) {
            // Choose a selection strategy based on the requested algorithm.
            return switch (algorithm)
            {

                // Return the following word from the pair with the highest count.
                case MOST_FREQUENT -> pairs.stream()
                        // Find the pair whose count is greatest.
                        .max(Comparator.comparingInt(WordPair::getCount))
                        // Convert the selected pair to its following word.
                        .map(WordPair::getFollowingWord);
                // Select a following word randomly, weighted by each pair's count.
                case PROBABILITY_WEIGHTED -> Optional.of(weightedPick(pairs));
                // Choose one of the pairs uniformly at random.
                case RANDOM_WALK -> Optional.of(
                        // Select a random pair and get its following word.
                        pairs.get(random.nextInt(pairs.size())).getFollowingWord());
                // Finish the switch expression and return its selected result.
            };
        }
        // Check whether fallback words are unavailable.
        if (fallback == null || fallback.isEmpty()) {
            // Report that no next word could be selected.
            return Optional.empty();
        }
        // Choose and return one fallback word at random.
        return Optional.of(fallback.get(random.nextInt(fallback.size())));
    }


    /**
     * Selects a following word randomly, with each pair's count determining
     * how likely its word is to be chosen.
     *
     * @param pairs follower pairs to select from
     * @return the selected following word
     * Method created by Claude AI
     * Prompt: Select a following word randomly, with each pair's count determining how likely its word is to be chosen.
     * Written by: Umair Malik(UAM200000) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private String weightedPick(List<WordPair> pairs) {
        // Add all pair counts to determine the total selection weight.
        int total = pairs.stream().mapToInt(WordPair::getCount).sum();
        // Pick a random number within the total weight (using 1 if total is zero).
        int roll = random.nextInt(Math.max(total, 1));
        // Track the cumulative weight as pairs are visited.
        int running = 0;
        // Check each pair to find which one contains the random roll in its weight range.
        for (WordPair pair : pairs) {
            // Add this pair's count to the cumulative weight.
            running += pair.getCount();
            // If the roll falls in this pair's range, choose its following word.
            if (roll < running) {
                // Return the selected pair's following word.
                return pair.getFollowingWord();
            }
        }
        // Return the last following word as a safeguard against rounding or zero weights.
        return pairs.get(pairs.size() - 1).getFollowingWord();
    }
}
