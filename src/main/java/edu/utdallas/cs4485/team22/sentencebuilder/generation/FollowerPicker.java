package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Chooses the next word from follower pairs using a generation algorithm.
 *
 * @author Umair Malik
 * @date 2026-10-16
 */
public final class FollowerPicker {

    private final Random random;

    /**
     * @param random shared random source (seeded by caller for tests)
     * @author Umair Malik
     * @date 2026-10-16
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
     * @author Umair Malik
     * @date 2026-10-16
     */
    public Optional<String> pick(List<WordPair> pairs, GenerationAlgorithm algorithm,
                                 List<String> fallback) {
        if (pairs != null && !pairs.isEmpty()) {
            return switch (algorithm) {
                case MOST_FREQUENT -> pairs.stream()
                        .max(Comparator.comparingInt(WordPair::getCount))
                        .map(WordPair::getFollowingWord);
                case PROBABILITY_WEIGHTED -> Optional.of(weightedPick(pairs));
                case RANDOM_WALK -> Optional.of(
                        pairs.get(random.nextInt(pairs.size())).getFollowingWord());
            };
        }
        if (fallback == null || fallback.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fallback.get(random.nextInt(fallback.size())));
    }

    private String weightedPick(List<WordPair> pairs) {
        int total = pairs.stream().mapToInt(WordPair::getCount).sum();
        int roll = random.nextInt(Math.max(total, 1));
        int running = 0;
        for (WordPair pair : pairs) {
            running += pair.getCount();
            if (roll < running) {
                return pair.getFollowingWord();
            }
        }
        return pairs.get(pairs.size() - 1).getFollowingWord();
    }
}
