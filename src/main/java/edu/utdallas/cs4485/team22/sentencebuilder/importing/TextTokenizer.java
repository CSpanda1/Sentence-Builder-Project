package edu.utdallas.cs4485.team22.sentencebuilder.importing;

/*****************************************************************************************************************
 * TextTokenizer.java
 *
 * Splits imported text into sentences and normalizes its words into tokens
 * for storage and sentence generation.
 *
 * AI Assistance ChatGPT Codex. Some methods were generated with ChatGPT Codex
 * Written by: Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting November 20th, 2026.
 ******************************************************************************************************************/


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.BreakIterator;
import java.util.regex.Pattern;


public final class TextTokenizer {

    // Matches sequences of letters, combining marks, digits, or apostrophes as possible word tokens.
    private static final Pattern WORD_CANDIDATE = Pattern.compile("[\\p{L}\\p{M}\\p{N}‘’']+");
    // Matches valid words that start with a letter and may contain letters, combining marks, or internal apostrophes.
    private static final Pattern WORD = Pattern.compile("[\\p{L}][\\p{L}\\p{M}]*(?:'[\\p{L}][\\p{L}\\p{M}]*)*");

    /**
     * Extracts words from a book sentence, normalizes apostrophes and casing,
     * and returns the valid word tokens.
     *
     * @param sentence sentence text to tokenize
     * @return normalized list of words
     * Method created by ChatGPT Codex
     * Prompt: Create a method that extracts candidate words from a sentence,
     * normalizes curly apostrophes and letter casing, filters invalid tokens,
     * and returns the valid words in order.
     * Written by: Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting November 15, 2026.
     */
    public static List<String> tokenizeBookSentence(String sentence) {
        // Create a list to collect the normalized words.
        List<String> words = new ArrayList<>();
        // Find possible word-like sequences in the sentence.
        var candidates = WORD_CANDIDATE.matcher(sentence);
        // Process each candidate found by the pattern.
        while (candidates.find()) {
            // Convert curly apostrophes to straight apostrophes for consistent processing.
            String candidate = candidates.group().replace('‘', '\'').replace('’', '\'');
            // Remove apostrophes at the ends and normalize the candidate to lowercase.
            String word = candidate.replaceAll("^'+|'+$", "").toLowerCase(Locale.ROOT);
            // Ignore a one-letter token that was formed from a leading or trailing apostrophe,
            // except for the valid English words "a" and "i".
            if ((candidate.startsWith("'") || candidate.endsWith("'")) && word.length() == 1
                    && !word.equals("a") && !word.equals("i")) {
                // Skip this candidate and continue with the next one.
                continue;
            }
            // Keep the candidate only if it matches the accepted word pattern.
            if (WORD.matcher(word).matches()) {
                // Add the valid normalized word to the result list.
                words.add(word);
            }
        }
        // Return all valid words in their original order.
        return words;
    }
    /**
     * Tokenizes raw file content into normalized word tokens.
     * Sentence-ending punctuation is kept as a trailing {@code .} on the token
     * so callers can mark start/end flags.
     *
     * @param content raw text
     * @return ordered token list (may be empty)
     * Method created by ChatGPT codex
     * Prompt: Create a method that tokenizes raw text into lowercase word tokens,
     * ignores blank or invalid tokens, and preserves sentence-ending punctuation
     * as a trailing period so callers can identify sentence boundaries.
     * Written by: Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting November 15, 2026.
     */
    public static List<String> tokenize(String content) {
        // Return an empty list when there is no usable input.
        if (content == null || content.isBlank()) {
            return List.of();
        }
        // Normalize curly apostrophes and replace unsupported characters with spaces.
        String cleaned = content
                .replace('\u2019', '\'')
                .replaceAll("[^A-Za-z0-9.'!?\\s]", " ");
        // Split the cleaned text into pieces separated by whitespace.
        String[] raw = cleaned.split("\\s+");
        // Create a list to collect the normalized tokens.
        List<String> tokens = new ArrayList<>();
        // Process each piece from the text.
        for (String piece : raw) {
            // Skip null or blank pieces.
            if (piece == null || piece.isBlank()) {
                continue;
            }
            // Remove whitespace around the piece.
            String t = piece.trim();
            // Record whether the piece ends with sentence-ending punctuation.
            boolean end = t.endsWith(".") || t.endsWith("!") || t.endsWith("?");
            // Remove sentence-ending punctuation before validating the word.
            String core = t.replaceAll("[.!?]+$", "");
            // Remove punctuation from the beginning and end, while allowing apostrophes.
            core = core.replaceAll("^[^A-Za-z0-9']+|[^A-Za-z0-9']+$", "");
            // Skip pieces that contain no word characters after cleanup.
            if (core.isEmpty()) {
                continue;
            }
            // Normalize the word to lowercase independently of the system locale.
            String normalized = core.toLowerCase(Locale.ROOT);
            // Add the token, restoring a period if the original piece ended a sentence.
            tokens.add(end ? normalized + "." : normalized);
        }
        // Return the tokens in the order they appeared in the input.
        return tokens;
    }

    /**
     * Splits raw text into sentences using break iterators. Sentences are trimmed and empty sentences are discarded.
     * @param content raw text
     * @return ordered sentence list
     *
     * Method created by ChatGPT codex
     * Prompt: Create a method that uses BreakIterator to split raw text into
     * sentences, normalizes whitespace, trims each sentence, discards empty
     * results, and returns the sentences in their original order.
     *
     * Written by: Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting November 15, 2026.
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
     *
     * Method created by ChatGPT codex
     * Prompt: Create a method that safely removes a trailing period from a
     * tokenizer output token, returns an empty string for null input, and
     * otherwise returns the token unchanged when it has no trailing period.
     *
     * Written by: Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting November 15, 2026.
     *
     */
    public static String bareWord(String token) {
        // Return an empty string when the input token is null.
        if (token == null) {
            return "";
        }
        // Remove the final period if present; otherwise return the token unchanged.
        return token.endsWith(".") ? token.substring(0, token.length() - 1) : token;
    }

    /**
     * @param token tokenizer output
     * @return true if the token ended a sentence
     *
     * Method created by ChatGPT codex
     * Prompt: Create a method that checks whether a tokenizer output token
     * ends with a period, returning false when the token is null.
     *
     * Written by: Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting November 16th, 2026.
     */
    public static boolean isSentenceEnd(String token) {
        // Return true only when the token is not null and ends with a period.
        return token != null && token.endsWith(".");
    }
}
