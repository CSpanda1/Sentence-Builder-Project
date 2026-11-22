package edu.utdallas.cs4485.team22.sentencebuilder.model;

/*****************************************************************************************************************
 * WordPair.java
 *
 * Represents two consecutive words observed in the corpus and how often
 * the leading word is followed by the next word.
 *
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 ******************************************************************************************************************/

/**
 * Domain object for a word-follows-word relationship with an observed count.
 *
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 */
public class WordPair {

    // Word that appears first in this pair.
    private final String leadingWord;
    // Word that immediately follows the leading word.
    private final String followingWord;
    // Number of times this consecutive word pair appears in the corpus.
    private int count;

    /**
     * Creates a follower pair.
     *
     * @param leadingWord   word that came first
     * @param followingWord word that followed
     * @param count         times this pair was observed
     *
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     *
     */
    public WordPair(String leadingWord, String followingWord, int count) {
        this.leadingWord = leadingWord;
        this.followingWord = followingWord;
        this.count = count;
    }

    /**
     * Returns the word that appears first in this pair.
     *
     * @return the leading word
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public String getLeadingWord() {
        return leadingWord;
    }

    /**
     * Returns the word that immediately follows the leading word.
     *
     * @return the following word
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public String getFollowingWord() {
        return followingWord;
    }

    /**
     * Returns how many times this word pair was observed.
     *
     * @return the pair's occurrence count
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public int getCount() {
        return count;
    }

    /**
     * Updates the number of times this word pair was observed.
     *
     * @param count the new occurrence count
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Increases the observed occurrence count of this word pair by one.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public void incrementCount() {
        this.count++;
    }
}
