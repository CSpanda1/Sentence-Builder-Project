package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import java.util.List;
import java.util.Random;

/**
 * Third algorithm: picks a uniform-random follower (random walk).
 *
 * @author Umair Malik
 * @author Nirav Rohra
 * @date 2026-10-23
 */
public class RandomWalkGenerator extends SentenceGenerator {

    public RandomWalkGenerator(FollowerProvider provider, Random random) {
        super(provider, random);
    }

    @Override
    protected Follower chooseNext(List<Follower> followers) {
        return followers.get(getRandom().nextInt(followers.size()));
    }

    @Override
    public String getName() {
        return "Random walk";
    }
}
