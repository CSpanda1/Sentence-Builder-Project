package generation;

/******************************************************************************
 * WordNormalizer
 *
 * Turns raw typed text into the form words are stored in. Lower case, with
 * anything that is not a letter, digit, or apostrophe stripped off. Every
 * part of the program that looks a word up should go through this so that
 * "Rabbit," typed on screen matches "rabbit" in the word table.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 30, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public final class WordNormalizer
{
    // Anything that is not a letter, a digit, or an apostrophe. Apostrophes
    // stay so that "don't" is one word.
    private static final String NOT_WORD_CHARACTERS = "[^\\p{L}\\p{N}']+";

    private WordNormalizer()
    {
        // Utility class, never built.
    }

    /**************************************************************************
     * Normalizes one word. Input: the raw text. Output: the lower case word
     * with punctuation removed, or an empty string if nothing is left.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public static String normalize(String rawWord)
    {
        String result = "";

        if (rawWord != null)
        {
            result = rawWord.toLowerCase().replaceAll(NOT_WORD_CHARACTERS, "");
        }

        return result;
    }
}
