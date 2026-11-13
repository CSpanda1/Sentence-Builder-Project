package edu.utdallas.cs4485.team22.sentencebuilder.autocomplete;

/**
 * Write-side contract for autocomplete corpus updates.
 *
 * @author Umair Malik
 * @date 2026-10-30
 */
public interface AutoCompleteRecorder {

    void addWordIfMissing(String wordText);

    void incrementFollowCount(String firstWord, String nextWord);
}
