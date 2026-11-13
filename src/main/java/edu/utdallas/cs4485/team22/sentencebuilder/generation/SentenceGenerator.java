package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Abstract sentence generator. Subclasses only implement {@link #chooseNext}.
 *
 * @author Umair Malik
 * @date 2026-10-23
 */
public abstract class SentenceGenerator {

    public static final int DEFAULT_MAX_WORDS = 20;
    private static final int MIN_WORDS_BEFORE_ENDING = 3;

    private final FollowerProvider provider;
    private final Random random;

    protected SentenceGenerator(FollowerProvider provider, Random random) {
        this.provider = provider;
        this.random = random;
    }

    protected abstract Follower chooseNext(List<Follower> followers);

    public abstract String getName();

    /**
     * Generates one sentence from a start word.
     *
     * @param startWord seed word
     * @param maxWords  maximum length
     * @return capitalized sentence ending with a period
     * @author Umair Malik
     * @date 2026-10-23
     */
    public String generate(String startWord, int maxWords) {
        List<String> words = new ArrayList<>();
        String currentWord = startWord;
        boolean keepGoing = currentWord != null && !currentWord.isBlank();

        while (keepGoing) {
            words.add(currentWord);
            List<Follower> followers = provider.getFollowers(currentWord);

            if (followers.isEmpty() || words.size() >= maxWords
                    || shouldEndSentence(currentWord, words.size())) {
                keepGoing = false;
            } else {
                currentWord = chooseNext(followers).getWordText();
            }
        }
        return joinWords(words);
    }

    /**
     * Batch generation from random start words.
     *
     * @author Umair Malik
     * @date 2026-10-23
     */
    public List<String> generateBatch(int count, int maxWords) {
        List<String> sentences = new ArrayList<>();
        while (sentences.size() < count) {
            String startWord = provider.getRandomStartWord();
            if (startWord == null) {
                break;
            }
            sentences.add(generate(startWord, maxWords));
        }
        return sentences;
    }

    private boolean shouldEndSentence(String wordText, int wordsSoFar) {
        if (wordsSoFar < MIN_WORDS_BEFORE_ENDING) {
            return false;
        }
        int totalCount = provider.getTotalCount(wordText);
        int endCount = provider.getEndCount(wordText);
        if (totalCount <= 0 || endCount <= 0) {
            return false;
        }
        double endChance = (double) endCount / totalCount;
        return random.nextDouble() < endChance;
    }

    private String joinWords(List<String> words) {
        String sentence = String.join(" ", words);
        if (!sentence.isEmpty()) {
            sentence = Character.toUpperCase(sentence.charAt(0))
                    + sentence.substring(1) + ".";
        }
        return sentence;
    }

    protected Random getRandom() {
        return random;
    }

    protected FollowerProvider getProvider() {
        return provider;
    }
}
