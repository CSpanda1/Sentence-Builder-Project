package edu.utdallas.cs4485.team22.sentencebuilder.generation;

import java.util.Locale;

/**
 * Shared word normalization for generation and autocomplete.
 *
 * @author Umair Malik
 * @date 2026-10-23
 */
public final class WordNormalizer {

    private WordNormalizer() {
    }

    /**
     * Lowercases and trims; converts curly apostrophes to straight ones.
     *
     * @param word raw word
     * @return normalized text, or empty string if null/blank
     * @author Umair Malik
     * @date 2026-10-23
     */
    public static String normalize(String word) {
        if (word == null) {
            return "";
        }
        return word.trim()
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .toLowerCase(Locale.ROOT);
    }
}
