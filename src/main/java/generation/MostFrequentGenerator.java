package generation;

import java.util.List;
import java.util.Random;

/******************************************************************************
 * MostFrequentGenerator
 *
 * The simplest algorithm. At every step it takes the follower that appeared
 * most often after the current word. Given the same start word it always
 * produces the same sentence, which makes it easy to check by hand and a
 * good baseline to compare the weighted algorithm against. Its weakness is
 * that rare followers never show up.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 23, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public class MostFrequentGenerator extends SentenceGenerator
{
    private static final String NAME = "Most frequent follower";

    /**************************************************************************
     * Builds the generator. Inputs: the data source and the random source,
     * which this algorithm only uses for the end of sentence roll.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public MostFrequentGenerator(FollowerProvider provider, Random random)
    {
        super(provider, random);
    }

    /**************************************************************************
     * Returns the follower with the highest follow count. On a tie the first
     * one in the list wins, so the result is stable. Input: the followers,
     * never empty. Output: the chosen follower.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    @Override
    protected Follower chooseNext(List<Follower> followers)
    {
        Follower best = followers.get(0);
        int ix = 1;

        while (ix < followers.size())
        {
            if (followers.get(ix).getFollowCount() > best.getFollowCount())
            {
                best = followers.get(ix);
            }
            ix++;
        }

        return best;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
