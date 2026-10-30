package generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/******************************************************************************
 * SentenceGenerator
 *
 * Base class for every sentence generation algorithm. It owns the part that
 * is the same for all algorithms: start from a word, keep asking the data
 * layer for followers, decide when to stop, and turn the word list into a
 * readable sentence. Each subclass only decides which follower to take next
 * by implementing chooseNext.
 *
 * To add a new algorithm, extend this class, implement chooseNext and
 * getName, and nothing else in the program has to change.
 *
 * Stopping rules, in order: the word has no followers, the sentence has hit
 * the maximum length, or a random roll against the word's end_count says
 * the sentence ends here. A word that ended sentences often in the imported
 * text is likely to end a generated one too.
 *
 * Written by Umair Malik (UAM200000) for CS4485.0W1, Sentence Builder project,
 * starting October 23, 2026.
 *
 * Comments were reworked by Claude Fable 5.1.
 ******************************************************************************/
public abstract class SentenceGenerator
{
    // Longest sentence allowed when the caller does not give a limit. Keeps a
    // run from looping forever on words that always have followers.
    public static final int DEFAULT_MAX_WORDS = 20;

    // A sentence shorter than this never ends on the end_count roll, so the
    // generator does not produce one or two word sentences.
    private static final int MIN_WORDS_BEFORE_ENDING = 3;

    private final FollowerProvider provider;
    private final Random random;

    /**************************************************************************
     * Builds a generator. Inputs: the data source for followers and counts,
     * and the random source. Passing the Random in lets tests seed it so a
     * run is repeatable. Output: a new generator.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    protected SentenceGenerator(FollowerProvider provider, Random random)
    {
        this.provider = provider;
        this.random = random;
    }

    /**************************************************************************
     * Picks the next word. Each algorithm supplies its own rule here.
     * Input: the followers of the current word, never empty. Output: the
     * chosen follower.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    protected abstract Follower chooseNext(List<Follower> followers);

    /**************************************************************************
     * Returns a short display name for the algorithm, for the UI drop down
     * and for the algorithm column of generated_sentence.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public abstract String getName();

    /**************************************************************************
     * Generates one sentence from a start word using the default length
     * limit. Input: the first word. Output: the finished sentence.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public String generate(String startWord)
    {
        return generate(startWord, DEFAULT_MAX_WORDS);
    }

    /**************************************************************************
     * Generates one sentence. Inputs: the first word and the most words the
     * sentence may contain. Output: the sentence, capitalized and ending with
     * a period. If the start word is unknown the sentence is just that word.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public String generate(String startWord, int maxWords)
    {
        List<String> words = new ArrayList<>();
        String currentWord = startWord;
        boolean keepGoing = currentWord != null && !currentWord.isBlank();

        // Walk the follows chain until a stopping rule fires. Each pass adds
        // the current word and asks the subclass for the next one.
        while (keepGoing)
        {
            words.add(currentWord);

            List<Follower> followers = provider.getFollowers(currentWord);

            if (followers.isEmpty() || words.size() >= maxWords
                    || shouldEndSentence(currentWord, words.size()))
            {
                keepGoing = false;
            }
            else
            {
                currentWord = chooseNext(followers).getWordText();
            }
        }

        return joinWords(words);
    }

    /**************************************************************************
     * Generates several sentences, each from a random start word. Used by the
     * batch screen. Inputs: how many sentences and the length limit for each.
     * Output: the list of sentences. The list is shorter than asked only when
     * the database has no start words at all.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    public List<String> generateBatch(int count, int maxWords)
    {
        List<String> sentences = new ArrayList<>();
        boolean keepGoing = true;

        while (sentences.size() < count && keepGoing)
        {
            String startWord = provider.getRandomStartWord();

            // No start word means an empty database. Stop rather than loop.
            if (startWord == null)
            {
                keepGoing = false;
            }
            else
            {
                sentences.add(generate(startWord, maxWords));
            }
        }

        return sentences;
    }

    /**************************************************************************
     * Decides whether the sentence should end on the current word. The chance
     * equals how often the word ended a sentence in the imported text, which
     * is end_count divided by total_count. Inputs: the current word and how
     * many words the sentence has so far. Output: true to stop.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    private boolean shouldEndSentence(String wordText, int wordsSoFar)
    {
        boolean endHere = false;

        if (wordsSoFar >= MIN_WORDS_BEFORE_ENDING)
        {
            int totalCount = provider.getTotalCount(wordText);
            int endCount = provider.getEndCount(wordText);

            // A word never seen before has no history to roll against.
            if (totalCount > 0 && endCount > 0)
            {
                double endChance = (double) endCount / totalCount;
                endHere = random.nextDouble() < endChance;
            }
        }

        return endHere;
    }

    /**************************************************************************
     * Joins the words into one string with a capital first letter and a
     * period at the end. Input: the words in order. Output: the sentence.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    private String joinWords(List<String> words)
    {
        String sentence = String.join(" ", words);

        if (!sentence.isEmpty())
        {
            sentence = Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1) + ".";
        }

        return sentence;
    }

    /**************************************************************************
     * Gives subclasses the shared random source so every algorithm draws
     * from the same seedable stream. Output: the Random.
     * Written by Umair Malik, starting October 23, 2026.
     **************************************************************************/
    protected Random getRandom()
    {
        return random;
    }
}
