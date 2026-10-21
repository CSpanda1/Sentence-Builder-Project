package edu.utdallas.cs4485.team22.sentencebuilder.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private TextTokenizer() {
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
