package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import java.util.List;
import java.util.Random;

/**
 * Probability-weighted next-word selection.
 *
 * @author Umair Malik
 * @date 2026-10-23
 */
public class WeightedRandomGenerator extends SentenceGenerator {

    public WeightedRandomGenerator(FollowerProvider provider, Random random) {
        super(provider, random);
    }

    @Override
    protected Follower chooseNext(List<Follower> followers) {
        int totalCount = followers.stream().mapToInt(Follower::getFollowCount).sum();
        int target = getRandom().nextInt(Math.max(totalCount, 1));
        int runningTotal = 0;
        for (Follower follower : followers) {
            runningTotal += follower.getFollowCount();
            if (target < runningTotal) {
                return follower;
            }
        }
        return followers.get(followers.size() - 1);
    }

    @Override
    public String getName() {
        return "Probability weighted";
    }
}
