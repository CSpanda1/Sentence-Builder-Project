package edu.utdallas.cs4485.team22.sentencebuilder.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.BreakIterator;
import java.util.regex.Pattern;

/**
 * Splits plain text into word tokens and detects sentence boundaries.
 *
 * <p>A token ending with {@code .}, {@code !}, or {@code ?} is treated as a
 * sentence end. Punctuation other than apostrophes inside words is stripped.</p>
 *
 * @author Ahmed Sherwani
 * @date 2026-10-16
 */
public final class TextTokenizer {

    private static final Pattern WORD_CANDIDATE = Pattern.compile("[\\p{L}\\p{M}\\p{N}‘’']+");
    private static final Pattern WORD = Pattern.compile("[\\p{L}][\\p{L}\\p{M}]*(?:'[\\p{L}][\\p{L}\\p{M}]*)*");

    /** Returns normalized book words, keeping apostrophes only inside words. */
    public static List<String> tokenizeBookSentence(String sentence) {
        List<String> words = new ArrayList<>();
        var candidates = WORD_CANDIDATE.matcher(sentence);
        while (candidates.find()) {
            String candidate = candidates.group().replace('‘', '\'').replace('’', '\'');
            String word = candidate.replaceAll("^'+|'+$", "").toLowerCase(Locale.ROOT);
            if ((candidate.startsWith("'") || candidate.endsWith("'")) && word.length() == 1
                    && !word.equals("a") && !word.equals("i")) {
                continue;
            }
            if (WORD.matcher(word).matches()) {
                words.add(word);
            }
        }
        return words;
    }
    /**
     * Tokenizes raw file content into normalized word tokens.
     * Sentence-ending punctuation is kept as a trailing {@code .} on the token
     * so callers can mark start/end flags.
     *
     * @param content raw text
     * @return ordered token list (may be empty)
     * @author Ahmed Sherwani
     * @date 2026-10-16
     */
    public static List<String> tokenize(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String cleaned = content
                .replace('\u2019', '\'')
                .replaceAll("[^A-Za-z0-9.'!?\\s]", " ");
        String[] raw = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String piece : raw) {
            if (piece == null || piece.isBlank()) {
                continue;
            }
            String t = piece.trim();
            boolean end = t.endsWith(".") || t.endsWith("!") || t.endsWith("?");
            String core = t.replaceAll("[.!?]+$", "");
            core = core.replaceAll("^[^A-Za-z0-9']+|[^A-Za-z0-9']+$", "");
            if (core.isEmpty()) {
                continue;
            }
            String normalized = core.toLowerCase(Locale.ROOT);
            tokens.add(end ? normalized + "." : normalized);
        }
        return tokens;
    }

    /**
     *  Splits raw text into sentences using break iterators. Sentences are trimmed and empty sentences are discarded.
     * @param content raw text
     * @return ordered sentence list
     */
    public static List<String> splitSentences(String content) {
        String text = content.replaceAll("\\s+", " ").strip();
        //Create a list to hold the sentences
        List<String> sentences = new ArrayList<>();

        // Use BreakIterator to find sentence boundaries
        BreakIterator boundaries = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        // Set the text to be analyzed
        boundaries.setText(text);

        // Get the first boundary
        int start = boundaries.first();

        // Iterate over the boundaries and extract sentences
        for (int end = boundaries.next(); end != BreakIterator.DONE; end = boundaries.next())
        {
            // Extract the sentence and trim it
            String sentence = text.substring(start, end).strip();
            // Add the sentence to the list if it's not empty
            if (!sentence.isEmpty())
            {
                sentences.add(sentence);
            }
            // Move to the next boundary
            start = end;
        }

        // Return the list of sentences
        return sentences;
    }

    /**
     * Strips a trailing sentence-end marker from a token.
     *
     * @param token tokenizer output
     * @return bare word text
     * @author Ahmed Sherwani
     * @date 2026-10-16
     */
    public static String bareWord(String token) {
        if (token == null) {
            return "";
        }
        return token.endsWith(".") ? token.substring(0, token.length() - 1) : token;
    }

    /**
     * @param token tokenizer output
     * @return true if the token ended a sentence
     * @author Ahmed Sherwani
     * @date 2026-10-16
     */
    public static boolean isSentenceEnd(String token) {
        return token != null && token.endsWith(".");
    }
}
