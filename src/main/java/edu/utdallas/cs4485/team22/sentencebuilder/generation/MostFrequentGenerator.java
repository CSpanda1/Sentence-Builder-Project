package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import java.util.List;
import java.util.Random;

/**
 * Always picks the most common follower of the current word.
 *
 * @author Umair Malik
 * @date 2026-10-23
 */
public class MostFrequentGenerator extends SentenceGenerator {

    public MostFrequentGenerator(FollowerProvider provider, Random random) {
        super(provider, random);
    }

    @Override
    protected Follower chooseNext(List<Follower> followers) {
        Follower best = followers.get(0);
        for (int i = 1; i < followers.size(); i++) {
            if (followers.get(i).getFollowCount() > best.getFollowCount()) {
                best = followers.get(i);
            }
        }
        return best;
    }

    @Override
    public String getName() {
        return "Most frequent follower";
    }
}
