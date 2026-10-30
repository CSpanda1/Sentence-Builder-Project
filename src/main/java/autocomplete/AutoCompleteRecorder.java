package autocomplete;

/******************************************************************************
 * AutoCompleteRecorder
 *
 * The two writes the auto-complete screen needs. The data access layer
 * implements this on top of the word and follows tables. Keeping writes
 * behind an interface means the suggestion logic never touches SQL, and
 * tests can swap in a fake that just records what was called.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 30, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public interface AutoCompleteRecorder
{
    /**************************************************************************
     * Adds a word to the word table if it is not there yet. Called when the
     * user types a word the corpus has never seen. Input: the normalized word.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    void addWordIfMissing(String wordText);

    /**************************************************************************
     * Adds one to the follow count for the pair, creating the pair with a
     * count of one if it does not exist. Called when the user picks a
     * suggested word. Inputs: the word before and the word chosen.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    void incrementFollowCount(String firstWord, String nextWord);
}
