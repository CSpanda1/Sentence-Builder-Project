package generation;

import java.util.List;
import java.util.Random;

/******************************************************************************
 * WeightedRandomGenerator
 *
 * The probability weighted algorithm the project requires. Each follower is
 * chosen with a chance equal to its share of the follow counts. If "cat"
 * followed "the" 30 times and "dog" 10 times, "cat" wins three times out of
 * four, but "dog" still appears one time in four. That is the point of this
 * algorithm: common words dominate, yet rare followers still get used, so
 * the sentences vary from run to run.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 23, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public class WeightedRandomGenerator extends SentenceGenerator
{
    private static final String NAME = "Probability weighted";

    /**************************************************************************
     * Builds the generator. Inputs: the data source and the random source
     * used for every weighted draw.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public WeightedRandomGenerator(FollowerProvider provider, Random random)
    {
        super(provider, random);
    }

    /**************************************************************************
     * Picks a follower in proportion to its follow count. Think of the counts
     * laid end to end on a line. A random point on that line lands inside one
     * follower's segment, and a longer segment is hit more often.
     * Input: the followers, never empty. Output: the chosen follower.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    @Override
    protected Follower chooseNext(List<Follower> followers)
    {
        // Every follow_count is at least 1, so the total is never zero.
        int totalCount = followers.stream().mapToInt(Follower::getFollowCount).sum();

        // Random point on the line, from 0 up to but not including total.
        int target = getRandom().nextInt(totalCount);
        int runningTotal = 0;
        int ix = 0;
        Follower chosen = null;

        // Walk the segments until the running total passes the target.
        while (chosen == null)
        {
            runningTotal += followers.get(ix).getFollowCount();

            if (target < runningTotal)
            {
                chosen = followers.get(ix);
            }
            ix++;
        }

        return chosen;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
