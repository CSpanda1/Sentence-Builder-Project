package edu.utdallas.cs4485.team22.sentencebuilder.service;

/*****************************************************************************************************************
 * JdbcDataService.java
 *
 * Implements the application's data operations using MySQL through JDBC,
 * including text imports, word-pair queries, sentence generation, and reports.
 *
 * AI Assistance: ChatGPT codex
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.db.DatabaseConnection;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.FollowerPicker;
import edu.utdallas.cs4485.team22.sentencebuilder.importing.TextTokenizer;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GeneratedSentence;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.model.ImportedFile;
import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

/**
 * MySQL-backed {@link DataService} using plain JDBC (WP3–WP6 + WP8–WP9).
 *
 *Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 */
public class JdbcDataService implements DataService {

    // Opens MySQL connections using the application's database configuration.
    private final DatabaseConnection database;
    // Selects following words during sentence generation using random choices.
    private final FollowerPicker picker = new FollowerPicker(new Random());
    // Signals an active import operation to stop when cancellation is requested.
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    /**
     * Creates a service that uses the provided helper for MySQL connections.
     * The helper is shared by the service's database operations.
     *
     * @param database connection helper configured for the target database
     *
     *  Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public JdbcDataService(DatabaseConnection database) {
        this.database = database;
    }

    /**
     * {@inheritDoc}
     *
     * Reads and tokenizes the text file, updates the MySQL word and word-pair
     * data in a transaction, and records the import in the database. Reports
     * progress through the callback and rolls back database changes on failure.
     * AI Assistance: ChatGPT Codex
     * Prompt: Implement a JDBC-backed text import method that reads a file,
     * tokenizes its sentences, aggregates word frequencies and consecutive
     * word-pair counts, and stores the results and import history in one
     * transaction. Report progress, support cancellation, and roll back changes
     * if the import fails.
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception {
        // Reset the cancellation flag before starting this import.
        cancelRequested.set(false);
        // Read the file contents and split them into sentences.
        String content = Files.readString(path);
        List<String> sentences = TextTokenizer.splitSentences(content);

        // Tokenize each sentence into normalized words.
        List<List<String>> sentenceTokens = sentences.stream()
                .map(TextTokenizer::tokenizeBookSentence)
                .toList();
        // Count all tokens in the file.
        int totalWords = sentenceTokens.stream().mapToInt(List::size).sum();
        // Reject the file if the tokenizer found no words.
        if (totalWords == 0) {
            throw new IllegalArgumentException("File contained no importable words");
        }

        // Store each word's frequency and boundary flags, and count consecutive word pairs.
        Map<String, WordCounts> words = new HashMap<>();
        Map<WordTransition, Integer> pairs = new HashMap<>();
        // Process tokens sentence by sentence so word pairs do not cross sentence boundaries.
        for (List<String> tokens : sentenceTokens) {
            // Stop the import if cancellation was requested.
            checkImportCancelled();
            // Track the previous token to create adjacent word pairs.
            String previous = null;
            // Update frequency, sentence-boundary data, and pair counts for every token.
            for (int i = 0; i < tokens.size(); i++) {
                // Get the current token.
                String word = tokens.get(i);
                // Find or create the accumulated statistics for this word.
                WordCounts counts = words.computeIfAbsent(word, ignored -> new WordCounts());
                // Count this occurrence and mark whether the word starts or ends a sentence.
                counts.frequency++;
                counts.start |= i == 0;
                counts.end |= i == tokens.size() - 1;
                // Count the transition from the preceding word, if there is one.
                if (previous != null) {
                    pairs.merge(new WordTransition(previous, word), 1, Integer::sum);
                }
                // Set this token as the preceding word for the next iteration.
                previous = word;
            }
        }
        // Report progress after token analysis is complete.
        if (progressCallback != null) {
            progressCallback.accept(0.1);
        }

        // Open a database connection and start a transaction for the import.
        try (Connection conn = database.open()) {
            conn.setAutoCommit(false);
            try {
                // Save word data and retrieve IDs for linking word pairs.
                Map<String, Long> wordIds = saveImportedWords(conn, words);
                // Report progress after saving words.
                if (progressCallback != null) {
                    progressCallback.accept(0.5);
                }
                // Save word-pair counts, then check whether the import was cancelled.
                saveImportedPairs(conn, pairs, wordIds);
                checkImportCancelled();

                // Create the import-history record using file details and word totals.
                ImportedFile record = insertImportedFile(
                        conn,
                        path.getFileName().toString(),
                        path.toAbsolutePath().toString(),
                        totalWords,
                        words.size());
                // Commit the transaction so all import changes are saved together.
                conn.commit();
                // Report successful completion.
                if (progressCallback != null) {
                    progressCallback.accept(1.0);
                }
                // Return the newly created import-history record.
                return record;
            } catch (Exception e) {
                // Undo transaction changes if any import step fails.
                conn.rollback();
                throw e;
            } finally {
                // Restore the connection's default auto-commit setting.
                conn.setAutoCommit(true);
            }
        }
    }

    // Maximum number of word records written in one database batch during import.
    private static final int IMPORT_BATCH_SIZE = 500;

    /**
     * Holds the aggregated statistics for one word while importing a text file.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Create a class to hold the aggregated statistics for one word while importing a text file.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static final class WordCounts {
        // Number of times the word appears in the imported text.
        private int frequency;
        // Whether the word appears at the beginning of any sentence.
        private boolean start;
        // Whether the word appears at the end of any sentence.
        private boolean end;
    }

    /**
     * Represents an adjacent pair of words found in the imported text.
     *
     * @param leading the word that appears first
     * @param following the word immediately after it
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a private record named WordTransition to represent an
     * adjacent word pair, with fields for the leading word and the following word.
     *
     * Written by:Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private record WordTransition(String leading, String following) {}

    /**
     * Saves imported word statistics in batches and returns each word's database
     * ID so the import can link word pairs to the correct records. Batching
     * reduces database calls, while upserts add counts to words already stored.
     *
     * @param conn active import transaction
     * @param words aggregated statistics for each imported word
     * @return map from imported word text to its database ID
     * @throws SQLException if a database operation fails or a saved word ID is missing
     * @throws InterruptedException if the import is cancelled
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Batch-upsert imported word frequencies and sentence-boundary flags,
     * then return the database ID for each word. Check for cancellation between batches.
     * Written by: Nirav Rohra(NXR220063) at the University of Texas at Dallas starting October 17th, 2026.
     */
    private Map<String, Long> saveImportedWords(Connection conn, Map<String, WordCounts> words)
            throws SQLException, InterruptedException {
        // Sort words so inserts and ID lookup are processed consistently.
        List<String> ordered = words.keySet().stream().sorted().toList();
        // Keep the database ID for each word so pair records can reference it.
        Map<String, Long> ids = new HashMap<>();
        // Split large imports into manageable database batches.
        for (int offset = 0; offset < ordered.size(); offset += IMPORT_BATCH_SIZE) {
            // Honor cancellation between batches.
            checkImportCancelled();
            List<String> batch = ordered.subList(offset, Math.min(offset + IMPORT_BATCH_SIZE, ordered.size()));
            // Insert new words or add imported counts and boundary flags to existing words.
            String insert = """
                    INSERT INTO words (word_text, frequency, is_sentence_start, is_sentence_end)
                    VALUES %s AS incoming
                    ON DUPLICATE KEY UPDATE
                        frequency = words.frequency + incoming.frequency,
                        is_sentence_start = words.is_sentence_start OR incoming.is_sentence_start,
                        is_sentence_end = words.is_sentence_end OR incoming.is_sentence_end
                    """.formatted(placeholders("(?, ?, ?, ?)", batch.size()));
            // Bind each word's text, frequency, and sentence-boundary data.
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                int index = 1;
                for (String word : batch) {
                    WordCounts counts = words.get(word);
                    ps.setString(index++, word);
                    ps.setInt(index++, counts.frequency);
                    ps.setBoolean(index++, counts.start);
                    ps.setBoolean(index++, counts.end);
                }
                ps.executeUpdate();
            }
            // Retrieve IDs for the inserted or previously existing words.
            String select = "SELECT word_text, word_id FROM words WHERE word_text IN ("
                    + placeholders("?", batch.size()) + ")";
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                for (int i = 0; i < batch.size(); i++) {
                    ps.setString(i + 1, batch.get(i));
                }
                try (ResultSet rows = ps.executeQuery()) {
                    while (rows.next()) {
                        ids.put(rows.getString("word_text"), rows.getLong("word_id"));
                    }
                }
            }
            // Resolve any names omitted by MySQL because of collation-equivalent spelling.
            for (String word : batch) {
                if (!ids.containsKey(word)) {
                    ids.put(word, findWordId(conn, word)
                            .orElseThrow(() -> new SQLException("Missing imported word ID")));
                }
            }
        }
        return ids;
    }

    /**
     * Saves adjacent-word pair counts in batches, linking each pair to the
     * database word IDs returned by {@link #saveImportedWords(Connection, Map)}.
     * This is needed to persist the corpus transitions used for suggestions
     * and sentence generation.
     *
     * @param conn active import transaction
     * @param pairs aggregated counts for each adjacent word pair
     * @param ids map from word text to its database ID
     * @throws SQLException if a database operation fails
     * @throws InterruptedException if the import is cancelled
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Batch-upsert imported adjacent-word counts using the saved word IDs
     * and the current JDBC transaction. Check for cancellation between batches.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void saveImportedPairs(Connection conn, Map<WordTransition, Integer> pairs,
                                   Map<String, Long> ids) throws SQLException, InterruptedException {
        // Convert the pair map to entries so it can be divided into batches.
        List<Map.Entry<WordTransition, Integer>> entries = new ArrayList<>(pairs.entrySet());
        // Write pair counts in bounded batches.
        for (int offset = 0; offset < entries.size(); offset += IMPORT_BATCH_SIZE) {
            // Stop between batches if the import was cancelled.
            checkImportCancelled();
            var batch = entries.subList(offset, Math.min(offset + IMPORT_BATCH_SIZE, entries.size()));
            // Insert each transition or add its count to an existing transition.
            String insert = """
                    INSERT INTO word_pairs (leading_word_id, following_word_id, follow_count)
                    VALUES %s AS incoming
                    ON DUPLICATE KEY UPDATE follow_count = word_pairs.follow_count + incoming.follow_count
                    """.formatted(placeholders("(?, ?, ?)", batch.size()));
            // Bind the leading/following word IDs and observed transition count.
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                int index = 1;
                for (var entry : batch) {
                    WordTransition pair = entry.getKey();
                    ps.setLong(index++, ids.get(pair.leading()));
                    ps.setLong(index++, ids.get(pair.following()));
                    ps.setInt(index++, entry.getValue());
                }
                ps.executeUpdate();
            }
        }
    }

    /**
     * Repeats an SQL placeholder tuple to match the number of rows in a batch.
     * This keeps batch insert statements sized to the supplied data.
     *
     * @param tuple placeholder group, such as {@code (?, ?)}
     * @param count number of times to repeat the tuple
     * @return comma-separated placeholder groups
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Build a comma-separated SQL placeholder list by repeating a
     * parameter tuple once for each row in the batch.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static String placeholders(String tuple, int count) {
        // Build a comma-separated sequence of identical SQL placeholder groups.
        return String.join(", ", Collections.nCopies(count, tuple));
    }

    /**
     * Checks both the service cancellation flag and thread interruption.
     * Import code calls this between batches so cancellation can stop work promptly.
     *
     * @throws InterruptedException if cancellation or interruption was requested
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Check the import cancellation flag and the current thread's
     * interrupted status. Throw InterruptedException if either is set.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void checkImportCancelled() throws InterruptedException {
        // Treat either explicit cancellation or thread interruption as a stop request.
        if (cancelRequested.get() || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Import cancelled");
        }
    }

    /**
     * Requests cancellation of the current or next import operation.
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Set the import cancellation flag so the importer can stop when it
     * next checks for cancellation.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public void cancelImport() {
        // Let the active import notice the cancellation at its next check.
        cancelRequested.set(true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a sentence from the normalized starting phrase, stores the
     * result, and feeds it back into the corpus so later suggestions and
     * generation can use it.</p>
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Generate a sentence from a starting phrase and a selected follower
     * algorithm. Count the phrase toward the requested length, start the follower lookup from its
     * last word, use fallback words when needed, and save the result with its duplicate status
     * before feeding it back into the corpus.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength) {
        // Normalize the starting phrase and reject missing input.
        String prefix = normalize(startWord).replaceAll("(?U)^\\s+|\\s+$", "");
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("Enter a start word or phrase.");
        }

        // Split the seed phrase and constrain generated sentence length.
        List<String> prefixWords = List.of(prefix.split("(?U)\\s+"));
        int limit = Math.max(2, Math.min(maxLength, 40));
        if (prefixWords.size() > limit) {
            throw new IllegalArgumentException("The starting phrase has more words than the selected word count.");
        }
        // Use the first phrase word as the seed and the last as the initial follower lookup.
        String seed = prefixWords.getFirst();
        String current = prefixWords.getLast();

        try (Connection conn = database.open()) {
            // Ensure the seed can be referenced in the corpus and mark it as a sentence start.
            ensureWordExists(conn, seed, true, false);

            // Start the result with the user's phrase and prepare fallback words.
            List<String> parts = new ArrayList<>(prefixWords);
            parts.set(0, capitalize(seed));
            List<String> fallback = listWordTexts(conn, 500);

            // Keep selecting followers until the length limit is reached or no next word exists.
            while (parts.size() < limit) {
                // Use valid single-word followers for the selected algorithm.
                List<WordPair> pairs = suggestNext(conn, current, 50).stream()
                        .filter(pair -> FollowerPicker.isSingleWord(pair.getFollowingWord()))
                        .toList();
                // Stop if neither observed followers nor fallback words provide a choice.
                Optional<String> next = picker.pick(pairs, algorithm, fallback);
                if (next.isEmpty()) {
                    break;
                }
                // Append the chosen word and use it for the next follower lookup.
                String follower = next.get();
                parts.add(follower);
                current = follower;
            }

            // Assemble the output and ensure it ends with sentence punctuation.
            String text = String.join(" ", parts);
            if (!text.endsWith(".")) {
                text = text + ".";
            }
            // Check history, save the sentence, and update the corpus with its transitions.
            boolean duplicate = sentenceExists(conn, text);
            GeneratedSentence stored = insertSentence(
                    conn, text, algorithm.getDisplayName(), seed, duplicate);

            feedSentenceIntoCorpus(conn, text);
            return stored;
        } catch (SQLException e) {
            throw new IllegalStateException("Generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates several sentences, choosing a corpus sentence-start word for
     * each one. This provides a convenient batch operation without duplicating
     * the single-sentence generation logic.
     *
     * @param count number of sentences requested
     * @param algorithm generation strategy
     * @param maxLength total word count per sentence
     * @return generated sentence records
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Generate a batch of sentences by randomly choosing corpus sentence-
     * start words, falling back to frequent words when necessary, and calling the single-sentence
     * generator for each result.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength) {
        List<String> starts;
        try (Connection conn = database.open()) {
            // Prefer known sentence-start words, falling back to frequent corpus words.
            starts = listSentenceStartWords(conn);
            if (starts.isEmpty()) {
                starts = listWordTexts(conn, 200);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Batch start lookup failed: " + e.getMessage(), e);
        }
        // A batch cannot be generated if neither sentence starts nor other words exist.
        if (starts.isEmpty()) {
            throw new IllegalStateException("Corpus is empty; import text first");
        }

        // Clamp the requested batch size and generate each sentence from a random seed.
        Random random = new Random();
        List<GeneratedSentence> batch = new ArrayList<>();
        int n = Math.max(1, Math.min(count, 50));
        for (int i = 0; i < n; i++) {
            String start = starts.get(random.nextInt(starts.size()));
            batch.add(generate(start, algorithm, maxLength));
        }
        return batch;
    }

    /**
     * Retrieves likely followers for a word using a new database connection.
     * The query is delegated to the connection-aware helper so it can also be
     * reused while another database operation is in progress.
     *
     * @param leadingWord word immediately before the caret
     * @param limit maximum suggestions to return
     * @return follower pairs ordered by observed frequency
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Open a JDBC connection, normalize the leading word, and delegate to
     * the existing follower query with the requested limit.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<WordPair> suggestNext(String leadingWord, int limit) {
        // Open a connection and delegate to the helper that performs the query.
        try (Connection conn = database.open()) {
            return suggestNext(conn, normalize(leadingWord), limit);
        } catch (SQLException e) {
            throw new IllegalStateException("Suggest failed: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a normalized word or increments its corpus frequency if it exists.
     * The returned record lets callers use the current stored word data.
     *
     * @param wordText word text to add or count again
     * @return the stored word record
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Normalize a word, insert it or increment its stored frequency, and
     * return the updated Word record.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public Word addOrIncrementWord(String wordText) {
        // Normalize input and reject an empty word before accessing the database.
        String key = normalize(wordText);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        try (Connection conn = database.open()) {
            // Increment or insert the word, then return its current stored record.
            upsertWord(conn, key, false, false);
            return findWord(conn, key).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Add word failed: " + e.getMessage(), e);
        }
    }

    /**
     *
     * @param leadingWord   previous word
     * @param followingWord chosen suggestion or typed word
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Define a data-service method that records a transition from a
     * leading word to the word selected or typed immediately after it, updating
     * the corpus's word-pair data.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public void recordFollow(String leadingWord, String followingWord) {
        // Normalize both sides of the selected word transition.
        String lead = normalize(leadingWord);
        String follow = normalize(followingWord);
        // Ignore incomplete transitions rather than storing invalid pair data.
        if (lead.isEmpty() || follow.isEmpty()) {
            return;
        }
        try (Connection conn = database.open()) {
            // Ensure both words exist, increment their pair count, and log the selection.
            long leadId = upsertWord(conn, lead, false, false);
            long followId = upsertWord(conn, follow, false, false);
            upsertPair(conn, leadId, followId);
            recordAutocompleteEvent(conn, leadId, followId, lead, follow, false);
        } catch (SQLException e) {
            throw new IllegalStateException("Record follow failed: " + e.getMessage(), e);
        }
    }

    /**
     * Lists corpus words, optionally matching a case-insensitive substring.
     * This supplies the word editor with either all words or a filtered view.
     *
     * @param filter optional filter text; null or blank means no filter
     * @return matching words ordered by text
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  List corpus words in alphabetical order, with an optional case-
     * insensitive substring filter for the word editor.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<Word> listWords(String filter) {
        // Normalize the optional search text for a case-insensitive lookup.
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        // Select all words or restrict results to those containing the filter.
        String sql = f.isEmpty()
                ? "SELECT word_id, word_text, frequency, is_sentence_start, is_sentence_end "
                + "FROM words ORDER BY word_text"
                : "SELECT word_id, word_text, frequency, is_sentence_start, is_sentence_end "
                + "FROM words WHERE word_text LIKE ? ORDER BY word_text";
        // Bind the filter when present and map each database row to a Word.
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!f.isEmpty()) {
                ps.setString(1, "%" + f + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Word> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapWord(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("List words failed: " + e.getMessage(), e);
        }
    }

    /**
     * Persists editable fields for an existing word record.
     *
     * @param word word with updated values; its ID identifies the database row
     * @return {@code true} if one row was updated, otherwise {@code false}
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Update a word's text, frequency, and sentence-boundary flags by
     * database ID and report whether the row was updated.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public boolean updateWord(Word word) {
        // Update the editable fields for the word identified by its database ID.
        String sql = "UPDATE words SET word_text = ?, frequency = ?, "
                + "is_sentence_start = ?, is_sentence_end = ? WHERE word_id = ?";
        // Bind normalized values and report whether exactly one row was changed.
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalize(word.getText()));
            ps.setInt(2, Math.max(1, word.getFrequency()));
            ps.setBoolean(3, word.isSentenceStart());
            ps.setBoolean(4, word.isSentenceEnd());
            ps.setLong(5, word.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Update word failed: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a corpus word by normalized text.
     *
     * @param text word text to find
     * @return the matching word, or empty if it is not in the corpus
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Normalize the input word and look it up using a JDBC connection.
     * Return an empty Optional when it isn't in the corpus.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public Optional<Word> findWord(String text) {
        // Normalize the search text and reuse the connection-aware lookup helper.
        try (Connection conn = database.open()) {
            return findWord(conn, normalize(text));
        } catch (SQLException e) {
            throw new IllegalStateException("Find word failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves import history for display in reports, newest import first.
     *
     * @return imported-file records ordered by import time descending
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Load the import history into ImportedFile records ordered from
     * newest to oldest.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<ImportedFile> listImportedFiles() {
        // Fetch import history from newest to oldest for display in reports.
        String sql = "SELECT file_id, file_name, file_path, word_count, unique_word_count, imported_at "
                + "FROM imported_files ORDER BY imported_at DESC";
        // Convert each result row into an ImportedFile model.
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ImportedFile> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new ImportedFile(
                        rs.getLong("file_id"),
                        rs.getString("file_name"),
                        rs.getString("file_path"),
                        rs.getInt("word_count"),
                        rs.getInt("unique_word_count"),
                        rs.getTimestamp("imported_at").toLocalDateTime()));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("List imports failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves generated sentence history for reports, newest first.
     *
     * @return generated sentences ordered by creation time descending
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Load generated-sentence history into GeneratedSentence records
     * ordered from newest to oldest.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<GeneratedSentence> listSentences() {
        // Fetch sentence history in reverse chronological order.
        String sql = "SELECT sentence_id, sentence_text, algorithm, start_word, is_duplicate, created_at "
                + "FROM generated_sentences ORDER BY created_at DESC";
        // Convert each result row into a GeneratedSentence model.
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<GeneratedSentence> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new GeneratedSentence(
                        rs.getLong("sentence_id"),
                        rs.getString("sentence_text"),
                        rs.getString("algorithm"),
                        rs.getString("start_word"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getBoolean("is_duplicate")));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("List sentences failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sums the frequencies of all stored words to report corpus token volume.
     *
     * @return total number of word occurrences
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Query the sum of stored word frequencies and return the total
     * corpus token count.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public int getTotalWordCount() {
        // Sum all recorded word frequencies, returning zero for an empty table.
        String sql = "SELECT COALESCE(SUM(frequency), 0) FROM words";
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Total count failed: " + e.getMessage(), e);
        }
    }

    /**
     * Counts stored word records to report corpus vocabulary size.
     *
     * @return number of distinct stored words
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Count the rows in the words table to report the size of the corpus
     * vocabulary.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public int getUniqueWordCount() {
        // Count stored word rows, each representing one distinct corpus word.
        String sql = "SELECT COUNT(*) FROM words";
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Unique count failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the most recently imported file name for the application status display.
     *
     * @return the latest import name, or empty if the import history is empty
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Return the file name of the newest import, or an empty Optional if
     * no imports exist.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public Optional<String> getLastImportName() {
        // Query the newest import record so the UI can show its file name.
        String sql = "SELECT file_name FROM imported_files ORDER BY imported_at DESC LIMIT 1";
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.ofNullable(rs.getString(1));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Last import lookup failed: " + e.getMessage(), e);
        }
    }

    // ---- SQL helpers ----

    /**
     * Queries the most frequent following words using an existing connection.
     * Reusing the caller's connection avoids opening another one inside a
     * transaction or other database operation.
     *
     * @param conn active database connection
     * @param leadingWord preceding word to match
     * @param limit maximum number of pairs to return
     * @return matching pairs ordered by descending count
     * @throws SQLException if the query fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Use the caller's JDBC connection to find the following words for a
     * given leading word, ordered by transition count and capped at the requested limit.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private List<WordPair> suggestNext(Connection conn, String leadingWord, int limit)
            throws SQLException {
        // Join pair IDs to word text and rank candidate followers by observed count.
        String sql = """
                SELECT w1.word_text AS leading_text, w2.word_text AS following_text, p.follow_count
                FROM word_pairs p
                JOIN words w1 ON w1.word_id = p.leading_word_id
                JOIN words w2 ON w2.word_id = p.following_word_id
                WHERE w1.word_text = ?
                ORDER BY p.follow_count DESC
                LIMIT ?
                """;
        // Bind the leading word and result limit, then map rows into WordPair objects.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, leadingWord);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<WordPair> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new WordPair(
                            rs.getString("leading_text"),
                            rs.getString("following_text"),
                            rs.getInt("follow_count")));
                }
                return list;
            }
        }
    }

    /**
     * Increments an existing word's frequency or inserts it if missing, while
     * preserving sentence-boundary flags. Returns its ID for linking pairs.
     *
     * @param conn connection to use for the update
     * @param wordText normalized word text
     * @param start whether the word occurs at a sentence start
     * @param end whether the word occurs at a sentence end
     * @return the word's database ID
     * @throws SQLException if the query or insert fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Insert a new word or increment the existing word's frequency, merge
     * its sentence-start and sentence-end flags, and return its database ID.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private long upsertWord(Connection conn, String wordText, boolean start, boolean end)
            throws SQLException {
        // Look up an existing word so its occurrence and boundary flags can be updated.
        Optional<Long> existing = findWordId(conn, wordText);
        if (existing.isPresent()) {
            // Increment its frequency and preserve any previously set boundary flags.
            String sql = """
                    UPDATE words
                    SET frequency = frequency + 1,
                        is_sentence_start = is_sentence_start OR ?,
                        is_sentence_end = is_sentence_end OR ?
                    WHERE word_id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, start);
                ps.setBoolean(2, end);
                ps.setLong(3, existing.get());
                ps.executeUpdate();
            }
            return existing.get();
        }
        // Insert a new word and request its generated database ID.
        String sql = """
                INSERT INTO words (word_text, frequency, is_sentence_start, is_sentence_end)
                VALUES (?, 1, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, wordText);
            ps.setBoolean(2, start);
            ps.setBoolean(3, end);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /**
     * Ensures a word exists without incrementing an existing word's frequency.
     * This is used to register a generation seed while retaining any boundary
     * flags supplied by the caller.
     *
     * @param conn connection to use
     * @param wordText normalized word text
     * @param start whether the word should be marked as a sentence start
     * @param end whether the word should be marked as a sentence end
     * @throws SQLException if the lookup or database update fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Ensure the supplied word exists and merge its sentence-boundary
     * flags without increasing its frequency if it already exists.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void ensureWordExists(Connection conn, String wordText, boolean start, boolean end)
            throws SQLException {
        // Insert a missing word, or update boundary flags without changing frequency.
        if (findWordId(conn, wordText).isEmpty()) {
            upsertWord(conn, wordText, start, end);
        } else if (start || end) {
            String sql = """
                    UPDATE words
                    SET is_sentence_start = is_sentence_start OR ?,
                        is_sentence_end = is_sentence_end OR ?
                    WHERE word_text = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, start);
                ps.setBoolean(2, end);
                ps.setString(3, wordText);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Inserts a word transition or increments its count if it already exists.
     * These counts support follower suggestions and sentence generation.
     *
     * @param conn connection to use
     * @param leadingId database ID of the preceding word
     * @param followingId database ID of the following word
     * @throws SQLException if the insert or update fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Insert the transition between two word IDs or increment its count
     * if the pair already exists.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void upsertPair(Connection conn, long leadingId, long followingId) throws SQLException {
        // Insert a new transition or increment the existing transition count.
        String sql = """
                INSERT INTO word_pairs (leading_word_id, following_word_id, follow_count)
                VALUES (?, ?, 1)
                ON DUPLICATE KEY UPDATE follow_count = follow_count + 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, leadingId);
            ps.setLong(2, followingId);
            ps.executeUpdate();
        }
    }

    /**
     * Inserts a file's import summary and creates the corresponding model
     * object for return to the caller.
     *
     * @param conn connection participating in the import transaction
     * @param name imported file name
     * @param path imported file path
     * @param wordCount total token count
     * @param uniqueCount number of distinct words
     * @return the inserted import-history record
     * @throws SQLException if the insert fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Insert an import-history row containing the file name, path, and
     * word counts, then return the ImportedFile model with its generated ID.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private ImportedFile insertImportedFile(Connection conn, String name, String path,
                                            int wordCount, int uniqueCount) throws SQLException {
        // Persist the import summary and retrieve the generated record ID.
        String sql = """
                INSERT INTO imported_files (file_name, file_path, word_count, unique_word_count)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, path);
            ps.setInt(3, wordCount);
            ps.setInt(4, uniqueCount);
            ps.executeUpdate();
            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
            // Build the model returned to the caller from the saved data.
            return new ImportedFile(id, name, path, wordCount, uniqueCount, LocalDateTime.now());
        }
    }

    /**
     * Saves a generated sentence and returns its model representation.
     *
     * @param conn connection to use
     * @param text generated sentence text
     * @param algorithm display name of the generation strategy
     * @param startWord starting word used to generate the sentence
     * @param duplicate whether the same sentence was already stored
     * @return the inserted sentence record
     * @throws SQLException if the insert fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Insert a generated sentence with its algorithm, starting word, and
     * duplicate flag, then return the GeneratedSentence model with its generated ID.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private GeneratedSentence insertSentence(Connection conn, String text, String algorithm,
                                             String startWord, boolean duplicate)
            throws SQLException {
        // Store the generated sentence metadata and request its database ID.
        String sql = """
                INSERT INTO generated_sentences
                    (sentence_text, algorithm, start_word, is_duplicate)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, text);
            ps.setString(2, algorithm);
            ps.setString(3, startWord);
            ps.setBoolean(4, duplicate);
            ps.executeUpdate();
            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
            // Return a model matching the inserted sentence record.
            return new GeneratedSentence(id, text, algorithm, startWord, LocalDateTime.now(), duplicate);
        }
    }

    /**
     * Checks whether generated-sentence history already contains this text,
     * allowing generation to flag duplicate output.
     *
     * @param conn connection to use
     * @param text sentence text to check
     * @return {@code true} if a matching sentence exists
     * @throws SQLException if the query fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Check whether the generated-sentence table already contains the
     * supplied sentence text.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private boolean sentenceExists(Connection conn, String text) throws SQLException {
        // Compare case-insensitively and stop after the first matching record.
        String sql = "SELECT 1 FROM generated_sentences WHERE LOWER(sentence_text) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Adds a generated sentence's tokens and adjacent pairs back into the
     * corpus so future generation can learn from generated output.
     *
     * @param conn connection to use
     * @param sentence generated sentence text
     * @throws SQLException if a database operation fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Tokenize a generated sentence and update corpus word frequencies,
     * sentence-boundary flags, and counts for adjacent word pairs.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void feedSentenceIntoCorpus(Connection conn, String sentence) throws SQLException {
        // Tokenize the sentence while retaining its sentence-ending marker.
        List<String> tokens = TextTokenizer.tokenize(sentence);
        boolean atStart = true;
        String previous = null;
        // Update each word and the transition from the preceding token.
        for (String token : tokens) {
            String word = TextTokenizer.bareWord(token);
            boolean end = TextTokenizer.isSentenceEnd(token);
            long wordId = upsertWord(conn, word, atStart, end);
            if (previous != null) {
                long prevId = findWordId(conn, previous).orElseThrow();
                upsertPair(conn, prevId, wordId);
            }
            previous = word;
            atStart = end;
        }
    }

    /**
     * Stores an autocomplete selection event for later tracking or reporting.
     *
     * @param conn connection to use
     * @param leadId database ID of the leading word
     * @param followId database ID of the selected word
     * @param lead leading word text
     * @param follow selected word text
     * @param unknown whether the selected word was previously unknown
     * @throws SQLException if the event insert fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Record an autocomplete selection with the leading and selected word
     * IDs, their text, and whether the selected word was previously unknown.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void recordAutocompleteEvent(Connection conn, long leadId, long followId,
                                         String lead, String follow, boolean unknown)
            throws SQLException {
        // Persist the selected word pair and whether the chosen word was unknown.
        String sql = """
                INSERT INTO autocomplete_events
                    (leading_word_id, selected_word_id, leading_text, selected_text, was_unknown)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, leadId);
            ps.setLong(2, followId);
            ps.setString(3, lead);
            ps.setString(4, follow);
            ps.setBoolean(5, unknown);
            ps.executeUpdate();
        }
    }

    /**
     * Finds a word's database ID, which is needed to store references from
     * word-pair records.
     *
     * @param conn connection to use
     * @param text normalized word text
     * @return the word ID, or empty if the word is absent
     * @throws SQLException if the query fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Find a normalized word's database ID using the caller's JDBC
     * connection and return an empty Optional if the word is absent.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private Optional<Long> findWordId(Connection conn, String text) throws SQLException {
        // Query by normalized text and return the ID only when the word exists.
        String sql = "SELECT word_id FROM words WHERE word_text = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong(1));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Loads a word record by text using an existing connection.
     *
     * @param conn connection to use
     * @param text normalized word text
     * @return the word record, or empty if it does not exist
     * @throws SQLException if the query fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Find a word by normalized text on the caller's JDBC connection and
     * map the matching row to a Word model when present.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private Optional<Word> findWord(Connection conn, String text) throws SQLException {
        // Select the fields needed to reconstruct a Word model.
        String sql = """
                SELECT word_id, word_text, frequency, is_sentence_start, is_sentence_end
                FROM words WHERE word_text = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapWord(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Retrieves frequent single-token words as fallback candidates during
     * sentence generation.
     *
     * @param conn connection to use
     * @param limit maximum candidates to return
     * @return word texts ordered by frequency descending
     * @throws SQLException if the query fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Return up to the requested number of single-token words ordered by
     * descending frequency for use as generation fallbacks.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private List<String> listWordTexts(Connection conn, int limit) throws SQLException {
        // Retrieve frequent single-token words as candidates for generation fallback.
        String sql = "SELECT word_text FROM words WHERE word_text NOT REGEXP '[[:space:]]' "
                + "ORDER BY frequency DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
                return list;
            }
        }
    }

    /**
     * Retrieves single-token words marked as sentence starts for batch
     * generation's starting-word selection.
     *
     * @param conn connection to use
     * @return sentence-start word texts
     * @throws SQLException if the query fails
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Fetch single-token words marked as sentence starts so batch
     * generation can choose its starting words.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private List<String> listSentenceStartWords(Connection conn) throws SQLException {
        // Select single-token words marked as valid sentence starts.
        String sql = "SELECT word_text FROM words WHERE is_sentence_start = 1 "
                + "AND word_text NOT REGEXP '[[:space:]]'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        }
    }

    /**
     * Converts the current database result row into a {@link Word} model.
     * This keeps column-to-model mapping consistent across queries.
     *
     * @param rs result set positioned at a word row
     * @return mapped word
     * @throws SQLException if a required column cannot be read
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Map the current JDBC result row to a Word using its ID, text,
     * frequency, and sentence-boundary flags.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static Word mapWord(ResultSet rs) throws SQLException {
        // Read the word columns from the current row into the model constructor.
        return new Word(
                rs.getLong("word_id"),
                rs.getString("word_text"),
                rs.getInt("frequency"),
                rs.getBoolean("is_sentence_start"),
                rs.getBoolean("is_sentence_end"));
    }

    /**
     * Trims and lowercases text consistently before corpus lookups and writes.
     *
     * @param text input text, possibly null
     * @return normalized text, or an empty string for null
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Trim and lowercase input using Locale.ROOT, returning an empty
     * string for null.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static String normalize(String text) {
        // Use an empty value for null input so callers can validate consistently.
        if (text == null) {
            return "";
        }
        // Trim surrounding whitespace and lowercase independently of system locale.
        return text.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Uppercases the first character for display at the beginning of a sentence.
     *
     * @param text input text, possibly null or empty
     * @return text with its first character capitalized, or the original input
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Uppercase the first character for display while preserving the rest
     * of the string and passing through null or empty input.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static String capitalize(String text) {
        // Leave null or empty text unchanged.
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Uppercase the first character and preserve the remainder.
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
