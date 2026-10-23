package generation;

/******************************************************************************
 * Follower
 *
 * One candidate next word for the generator. Holds the word text and how
 * many times it was seen right after the current word, which is the
 * follow_count column of the follows table. The generators only need these
 * two facts to choose, so this is all the data layer has to hand them.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 23, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public class Follower
{
    private final String wordText;
    private final int followCount;

    /**************************************************************************
     * Builds a follower. Inputs: the next word's text and the number of times
     * it followed the current word. Output: a new Follower.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public Follower(String wordText, int followCount)
    {
        this.wordText = wordText;
        this.followCount = followCount;
    }

    public String getWordText()
    {
        return wordText;
    }

    public int getFollowCount()
    {
        return followCount;
    }
}
