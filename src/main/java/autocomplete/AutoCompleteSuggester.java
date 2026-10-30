package autocomplete;

import generation.Follower;
import generation.FollowerProvider;
import generation.WordNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/******************************************************************************
 * AutoCompleteSuggester
 *
 * The brains of the auto-complete screen. The UI hands it the text typed so
 * far and gets back a ranked list of next words, or nothing if it is not
 * time to suggest yet. Suggestions only appear right after a space, period,
 * or comma, because that is the moment the previous word is finished.
 *
 * The UI calls three methods:
 *   suggest           after every keystroke, to refresh the list
 *   acceptSuggestion  when the user picks a word from the list
 *   recordTypedWord   when the user finishes a word on their own
 *
 * The last two update the corpus through AutoCompleteRecorder, so words the
 * user types get added and words the user picks make that pair more likely
 * next time.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 30, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public class AutoCompleteSuggester
{
    // Characters that mark the end of a word. Typing one of these is the
    // signal that it is time to suggest.
    private static final String TRIGGER_CHARACTERS = " .,";

    // How many suggestions to return when the caller does not say.
    public static final int DEFAULT_MAX_SUGGESTIONS = 5;

    private final FollowerProvider provider;
    private final AutoCompleteRecorder recorder;

    /**************************************************************************
     * Builds the suggester. Inputs: the read side (followers and counts) and
     * the write side (adding words and bumping follow counts).
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public AutoCompleteSuggester(FollowerProvider provider, AutoCompleteRecorder recorder)
    {
        this.provider = provider;
        this.recorder = recorder;
    }

    /**************************************************************************
     * Suggests next words for the text typed so far, using the default list
     * size. Input: the full text in the box. Output: ranked next words.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public List<String> suggest(String typedText)
    {
        return suggest(typedText, DEFAULT_MAX_SUGGESTIONS);
    }

    /**************************************************************************
     * Suggests next words for the text typed so far. Inputs: the full text in
     * the box and the most suggestions to return. Output: the followers of
     * the last complete word, most frequent first, or an empty list if the
     * text does not end with a trigger character or the word is unknown.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public List<String> suggest(String typedText, int maxSuggestions)
    {
        List<String> suggestions = new ArrayList<>();

        if (endsWithTrigger(typedText))
        {
            String lastWord = lastCompleteWord(typedText);

            if (!lastWord.isEmpty())
            {
                List<Follower> followers = new ArrayList<>(provider.getFollowers(lastWord));

                // Most frequent first. Ties go alphabetically so the list
                // does not jump around between keystrokes.
                followers.sort(Comparator.comparingInt(Follower::getFollowCount).reversed()
                        .thenComparing(Follower::getWordText));

                int ix = 0;
                while (ix < followers.size() && ix < maxSuggestions)
                {
                    suggestions.add(followers.get(ix).getWordText());
                    ix++;
                }
            }
        }

        return suggestions;
    }

    /**************************************************************************
     * Records that the user picked a suggested word. The pair (last complete
     * word, chosen word) gets one more follow count, so the suggestion ranks
     * higher next time. Inputs: the text that was in the box when the list
     * was shown, and the word picked. Output: the chosen word, normalized,
     * ready for the UI to append to the text.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public String acceptSuggestion(String typedText, String chosenWord)
    {
        String lastWord = lastCompleteWord(typedText);
        String chosen = WordNormalizer.normalize(chosenWord);

        if (!lastWord.isEmpty() && !chosen.isEmpty())
        {
            recorder.incrementFollowCount(lastWord, chosen);
        }

        return chosen;
    }

    /**************************************************************************
     * Records a word the user typed themselves rather than picking from the
     * list. If the corpus has never seen it, it is added so it can be
     * suggested in future. Input: the raw word as typed. Output: true if the
     * word was new to the corpus.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public boolean recordTypedWord(String rawWord)
    {
        String word = WordNormalizer.normalize(rawWord);
        boolean wasNew = false;

        if (!word.isEmpty() && provider.getTotalCount(word) == 0)
        {
            recorder.addWordIfMissing(word);
            wasNew = true;
        }

        return wasNew;
    }

    /**************************************************************************
     * Finds the last finished word in the text. Trailing trigger characters
     * are ignored, so "the rabbit, " gives "rabbit". Input: the typed text.
     * Output: the normalized word, or an empty string if there is none.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    public String lastCompleteWord(String typedText)
    {
        String result = "";

        if (typedText != null)
        {
            String[] pieces = typedText.trim().split("\\s+");
            int ix = pieces.length - 1;

            // Walk back past pieces that normalize to nothing, such as a
            // lone comma, until a real word turns up.
            while (ix >= 0 && result.isEmpty())
            {
                result = WordNormalizer.normalize(pieces[ix]);
                ix--;
            }
        }

        return result;
    }

    /**************************************************************************
     * Tells whether the text ends with a space, period, or comma. Input: the
     * typed text. Output: true if it is time to suggest.
     * Written by Umair Malik, starting October 30, 2026.
     **************************************************************************/
    private boolean endsWithTrigger(String typedText)
    {
        boolean ends = false;

        if (typedText != null && !typedText.isEmpty())
        {
            char lastChar = typedText.charAt(typedText.length() - 1);
            ends = TRIGGER_CHARACTERS.indexOf(lastChar) >= 0;
        }

        return ends;
    }
}
