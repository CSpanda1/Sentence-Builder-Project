package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;

import java.util.Random;

/**
 * Builds the concrete generator for a UI algorithm choice.
 *
 * @author Umair Malik
 * @author Nirav Rohra
 * @date 2026-11-13
 */
public final class GeneratorFactory {

    private GeneratorFactory() {
    }

    /**
     * @param algorithm UI selection
     * @param provider  follower data source
     * @param random    shared random
     * @return concrete generator
     */
    public static SentenceGenerator create(GenerationAlgorithm algorithm,
                                           FollowerProvider provider,
                                           Random random) {
        return switch (algorithm) {
            case MOST_FREQUENT -> new MostFrequentGenerator(provider, random);
            case RANDOM_WALK -> new RandomWalkGenerator(provider, random);
            case PROBABILITY_WEIGHTED -> new WeightedRandomGenerator(provider, random);
        };
    }
}
