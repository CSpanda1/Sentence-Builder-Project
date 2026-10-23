package generation;

import java.util.List;

/******************************************************************************
 * FollowerProvider
 *
 * The only thing a sentence generator needs from the rest of the program.
 * The data access layer implements this interface on top of the word and
 * follows tables. Keeping the generators behind an interface means they
 * never touch SQL, which follows the three layer rule, and it lets tests
 * feed them a small in memory word list instead of a database.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 23, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public interface FollowerProvider
{
    /**************************************************************************
     * Returns every word that has been seen right after the given word, with
     * its follow count. Input: the current word's text. Output: the list of
     * followers, empty if the word is unknown or nothing ever followed it.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    List<Follower> getFollowers(String wordText);

    /**************************************************************************
     * Returns how many times the word has been seen in total, the total_count
     * column. Input: the word's text. Output: the count, 0 for an unknown word.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    int getTotalCount(String wordText);

    /**************************************************************************
     * Returns how many times the word ended a sentence, the end_count column.
     * Input: the word's text. Output: the count, 0 for an unknown word.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    int getEndCount(String wordText);

    /**************************************************************************
     * Returns a word that has started at least one sentence, chosen at random.
     * Used by batch generation when the user does not supply a start word.
     * Output: the word's text, or null if the database has no words yet.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    String getRandomStartWord();
}
