package edu.utdallas.cs4485.team22.sentencebuilder.model;

/**
 * Domain object for a word-follows-word relationship with an observed count.
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class WordPair {

    private final String leadingWord;
    private final String followingWord;
    private int count;

    /**
     * Creates a follower pair.
     *
     * @param leadingWord   word that came first
     * @param followingWord word that followed
     * @param count         times this pair was observed
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public WordPair(String leadingWord, String followingWord, int count) {
        this.leadingWord = leadingWord;
        this.followingWord = followingWord;
        this.count = count;
    }

    public String getLeadingWord() {
        return leadingWord;
    }

    public String getFollowingWord() {
        return followingWord;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        this.count++;
    }
}
