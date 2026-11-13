package edu.utdallas.cs4485.team22.sentencebuilder.generation;

/**
 * One word that has been seen following another, with its follow count.
 *
 * @author Umair Malik
 * @date 2026-10-23
 */
public class Follower {

    private final String wordText;
    private final int followCount;

    /**
     * @param wordText    follower word
     * @param followCount times this pair was observed
     * @author Umair Malik
     * @date 2026-10-23
     */
    public Follower(String wordText, int followCount) {
        this.wordText = wordText;
        this.followCount = followCount;
    }

    public String getWordText() {
        return wordText;
    }

    public int getFollowCount() {
        return followCount;
    }
}
