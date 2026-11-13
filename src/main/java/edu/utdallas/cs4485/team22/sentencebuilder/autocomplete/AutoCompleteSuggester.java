package edu.utdallas.cs4485.team22.sentencebuilder.autocomplete;

import edu.utdallas.cs4485.team22.sentencebuilder.generation.Follower;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.FollowerProvider;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.WordNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Next-word suggestions after space, period, or comma.
 *
 * @author Umair Malik
 * @date 2026-10-30
 */
public class AutoCompleteSuggester {

    private static final String TRIGGER_CHARACTERS = " .,";
    public static final int DEFAULT_MAX_SUGGESTIONS = 12;

    private final FollowerProvider provider;
    private final AutoCompleteRecorder recorder;

    public AutoCompleteSuggester(FollowerProvider provider, AutoCompleteRecorder recorder) {
        this.provider = provider;
        this.recorder = recorder;
    }

    public List<String> suggest(String typedText) {
        return suggest(typedText, DEFAULT_MAX_SUGGESTIONS);
    }

    /**
     * @param typedText       full compose text
     * @param maxSuggestions  max results
     * @return ranked next words, or empty if not at a trigger
     * @author Umair Malik
     * @date 2026-10-30
     */
    public List<String> suggest(String typedText, int maxSuggestions) {
        List<String> suggestions = new ArrayList<>();
        if (!endsWithTrigger(typedText)) {
            return suggestions;
        }
        String lastWord = lastCompleteWord(typedText);
        if (lastWord.isEmpty()) {
            return suggestions;
        }
        List<Follower> followers = new ArrayList<>(provider.getFollowers(lastWord));
        followers.sort(Comparator.comparingInt(Follower::getFollowCount).reversed()
                .thenComparing(Follower::getWordText));
        for (int i = 0; i < followers.size() && i < maxSuggestions; i++) {
            suggestions.add(followers.get(i).getWordText());
        }
        return suggestions;
    }

    /**
     * Records an accepted suggestion and returns the normalized chosen word.
     *
     * @author Umair Malik
     * @date 2026-10-30
     */
    public String acceptSuggestion(String typedText, String chosenWord) {
        String lastWord = lastCompleteWord(typedText);
        String chosen = WordNormalizer.normalize(chosenWord);
        if (!lastWord.isEmpty() && !chosen.isEmpty()) {
            recorder.incrementFollowCount(lastWord, chosen);
        }
        return chosen;
    }

    /**
     * Adds an unknown typed word and optionally links it to the previous word.
     *
     * @author Umair Malik
     * @date 2026-10-30
     */
    public String recordTypedWord(String typedText, String newWord) {
        String chosen = WordNormalizer.normalize(newWord);
        if (chosen.isEmpty()) {
            return "";
        }
        recorder.addWordIfMissing(chosen);
        String lastWord = lastCompleteWord(typedText);
        if (!lastWord.isEmpty()) {
            recorder.incrementFollowCount(lastWord, chosen);
        }
        return chosen;
    }

    public String lastCompleteWord(String typedText) {
        if (typedText == null || typedText.isBlank()) {
            return "";
        }
        String trimmed = typedText.replaceAll("[\\s.,]+$", "");
        if (trimmed.isEmpty()) {
            return "";
        }
        int idx = trimmed.length() - 1;
        while (idx >= 0 && !Character.isWhitespace(trimmed.charAt(idx))
                && trimmed.charAt(idx) != '.' && trimmed.charAt(idx) != ',') {
            idx--;
        }
        return WordNormalizer.normalize(trimmed.substring(idx + 1));
    }

    private boolean endsWithTrigger(String typedText) {
        if (typedText == null || typedText.isEmpty()) {
            return false;
        }
        return TRIGGER_CHARACTERS.indexOf(typedText.charAt(typedText.length() - 1)) >= 0;
    }
}
