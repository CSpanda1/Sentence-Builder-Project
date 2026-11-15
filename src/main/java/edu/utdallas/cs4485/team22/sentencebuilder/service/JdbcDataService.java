package edu.utdallas.cs4485.team22.sentencebuilder.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

/**
 * MySQL-backed {@link DataService} using plain JDBC (WP3–WP6 + WP8–WP9).
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class JdbcDataService implements DataService {

    private final DatabaseConnection database;
    private final FollowerPicker picker = new FollowerPicker(new Random());
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    /**
     * @param database connection helper
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public JdbcDataService(DatabaseConnection database) {
        this.database = database;
    }

    /**
     * {@inheritDoc}
     *
     * @author Ahmed Sherwani
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception {
        cancelRequested.set(false);
        String content = Files.readString(path);
        List<String> sentences = TextTokenizer.splitSentences(content);

        List<List<String>> sentenceTokens = sentences.stream()
                .map(TextTokenizer::tokenizeBookSentence)
                .toList();
        int totalWords = sentenceTokens.stream().mapToInt(List::size).sum();
        if (totalWords == 0) {
            throw new IllegalArgumentException("File contained no importable words");
        }

        Map<String, WordCounts> words = new HashMap<>();
        Map<WordTransition, Integer> pairs = new HashMap<>();
        for (List<String> tokens : sentenceTokens) {
            checkImportCancelled();
            String previous = null;
            for (int i = 0; i < tokens.size(); i++) {
                String word = tokens.get(i);
                WordCounts counts = words.computeIfAbsent(word, ignored -> new WordCounts());
                counts.frequency++;
                counts.start |= i == 0;
                counts.end |= i == tokens.size() - 1;
                if (previous != null) {
                    pairs.merge(new WordTransition(previous, word), 1, Integer::sum);
                }
                previous = word;
            }
        }
        if (progressCallback != null) {
            progressCallback.accept(0.1);
        }

        try (Connection conn = database.open()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Long> wordIds = saveImportedWords(conn, words);
                if (progressCallback != null) {
                    progressCallback.accept(0.5);
                }
                saveImportedPairs(conn, pairs, wordIds);
                checkImportCancelled();

                ImportedFile record = insertImportedFile(
                        conn,
                        path.getFileName().toString(),
                        path.toAbsolutePath().toString(),
                        totalWords,
                        words.size());
                conn.commit();
                if (progressCallback != null) {
                    progressCallback.accept(1.0);
                }
                return record;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static final int IMPORT_BATCH_SIZE = 500;

    private static final class WordCounts {
        private int frequency;
        private boolean start;
        private boolean end;
    }

    private record WordTransition(String leading, String following) {}

    private Map<String, Long> saveImportedWords(Connection conn, Map<String, WordCounts> words)
            throws SQLException, InterruptedException {
        List<String> ordered = words.keySet().stream().sorted().toList();
        Map<String, Long> ids = new HashMap<>();
        for (int offset = 0; offset < ordered.size(); offset += IMPORT_BATCH_SIZE) {
            checkImportCancelled();
            List<String> batch = ordered.subList(offset, Math.min(offset + IMPORT_BATCH_SIZE, ordered.size()));
            String insert = """
                    INSERT INTO words (word_text, frequency, is_sentence_start, is_sentence_end)
                    VALUES %s AS incoming
                    ON DUPLICATE KEY UPDATE
                        frequency = words.frequency + incoming.frequency,
                        is_sentence_start = words.is_sentence_start OR incoming.is_sentence_start,
                        is_sentence_end = words.is_sentence_end OR incoming.is_sentence_end
                    """.formatted(placeholders("(?, ?, ?, ?)", batch.size()));
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
            // An existing row may have a spelling equivalent under MySQL's collation.
            for (String word : batch) {
                if (!ids.containsKey(word)) {
                    ids.put(word, findWordId(conn, word)
                            .orElseThrow(() -> new SQLException("Missing imported word ID")));
                }
            }
        }
        return ids;
    }

    private void saveImportedPairs(Connection conn, Map<WordTransition, Integer> pairs,
                                   Map<String, Long> ids) throws SQLException, InterruptedException {
        List<Map.Entry<WordTransition, Integer>> entries = new ArrayList<>(pairs.entrySet());
        for (int offset = 0; offset < entries.size(); offset += IMPORT_BATCH_SIZE) {
            checkImportCancelled();
            var batch = entries.subList(offset, Math.min(offset + IMPORT_BATCH_SIZE, entries.size()));
            String insert = """
                    INSERT INTO word_pairs (leading_word_id, following_word_id, follow_count)
                    VALUES %s AS incoming
                    ON DUPLICATE KEY UPDATE follow_count = word_pairs.follow_count + incoming.follow_count
                    """.formatted(placeholders("(?, ?, ?)", batch.size()));
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

    private static String placeholders(String tuple, int count) {
        return String.join(", ", Collections.nCopies(count, tuple));
    }

    private void checkImportCancelled() throws InterruptedException {
        if (cancelRequested.get() || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Import cancelled");
        }
    }

    @Override
    public void cancelImport() {
        cancelRequested.set(true);
    }

    /**
     * {@inheritDoc}
     *
     * @author Umair Malik
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength) {
        String seed = normalize(startWord);
        if (seed.isEmpty()) {
            throw new IllegalArgumentException("Start word is required");
        }

        try (Connection conn = database.open()) {
            ensureWordExists(conn, seed, true, false);

            List<String> parts = new ArrayList<>();
            parts.add(capitalize(seed));
            String current = seed;
            int limit = Math.max(2, Math.min(maxLength, 40));
            List<String> fallback = listWordTexts(conn, 500);

            for (int i = 1; i < limit; i++) {
                List<WordPair> pairs = suggestNext(conn, current, 50);
                Optional<String> next = picker.pick(pairs, algorithm, fallback);
                if (next.isEmpty()) {
                    break;
                }
                String follower = next.get();
                parts.add(follower);
                current = follower;
                Optional<Word> w = findWord(conn, follower);
                if (w.isPresent() && w.get().isSentenceEnd() && i > 2) {
                    break;
                }
            }

            String text = String.join(" ", parts);
            if (!text.endsWith(".")) {
                text = text + ".";
            }
            boolean duplicate = sentenceExists(conn, text);
            GeneratedSentence stored = insertSentence(
                    conn, text, algorithm.getDisplayName(), seed, duplicate);

            // Feed generated sentence back into the corpus (plan WP6).
            feedSentenceIntoCorpus(conn, text);
            return stored;
        } catch (SQLException e) {
            throw new IllegalStateException("Generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength) {
        List<String> starts;
        try (Connection conn = database.open()) {
            starts = listSentenceStartWords(conn);
            if (starts.isEmpty()) {
                starts = listWordTexts(conn, 200);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Batch start lookup failed: " + e.getMessage(), e);
        }
        if (starts.isEmpty()) {
            throw new IllegalStateException("Corpus is empty; import text first");
        }

        Random random = new Random();
        List<GeneratedSentence> batch = new ArrayList<>();
        int n = Math.max(1, Math.min(count, 50));
        for (int i = 0; i < n; i++) {
            String start = starts.get(random.nextInt(starts.size()));
            batch.add(generate(start, algorithm, maxLength));
        }
        return batch;
    }

    @Override
    public List<WordPair> suggestNext(String leadingWord, int limit) {
        try (Connection conn = database.open()) {
            return suggestNext(conn, normalize(leadingWord), limit);
        } catch (SQLException e) {
            throw new IllegalStateException("Suggest failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Word addOrIncrementWord(String wordText) {
        String key = normalize(wordText);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        try (Connection conn = database.open()) {
            upsertWord(conn, key, false, false);
            return findWord(conn, key).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Add word failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void recordFollow(String leadingWord, String followingWord) {
        String lead = normalize(leadingWord);
        String follow = normalize(followingWord);
        if (lead.isEmpty() || follow.isEmpty()) {
            return;
        }
        try (Connection conn = database.open()) {
            long leadId = upsertWord(conn, lead, false, false);
            long followId = upsertWord(conn, follow, false, false);
            upsertPair(conn, leadId, followId);
            recordAutocompleteEvent(conn, leadId, followId, lead, follow, false);
        } catch (SQLException e) {
            throw new IllegalStateException("Record follow failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Word> listWords(String filter) {
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        String sql = f.isEmpty()
                ? "SELECT word_id, word_text, frequency, is_sentence_start, is_sentence_end "
                + "FROM words ORDER BY word_text"
                : "SELECT word_id, word_text, frequency, is_sentence_start, is_sentence_end "
                + "FROM words WHERE word_text LIKE ? ORDER BY word_text";
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

    @Override
    public boolean updateWord(Word word) {
        String sql = "UPDATE words SET word_text = ?, frequency = ?, "
                + "is_sentence_start = ?, is_sentence_end = ? WHERE word_id = ?";
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

    @Override
    public Optional<Word> findWord(String text) {
        try (Connection conn = database.open()) {
            return findWord(conn, normalize(text));
        } catch (SQLException e) {
            throw new IllegalStateException("Find word failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImportedFile> listImportedFiles() {
        String sql = "SELECT file_id, file_name, file_path, word_count, unique_word_count, imported_at "
                + "FROM imported_files ORDER BY imported_at DESC";
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

    @Override
    public List<GeneratedSentence> listSentences() {
        String sql = "SELECT sentence_id, sentence_text, algorithm, start_word, is_duplicate, created_at "
                + "FROM generated_sentences ORDER BY created_at DESC";
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

    @Override
    public int getTotalWordCount() {
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

    @Override
    public int getUniqueWordCount() {
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

    @Override
    public Optional<String> getLastImportName() {
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

    private List<WordPair> suggestNext(Connection conn, String leadingWord, int limit)
            throws SQLException {
        String sql = """
                SELECT w1.word_text AS leading_text, w2.word_text AS following_text, p.follow_count
                FROM word_pairs p
                JOIN words w1 ON w1.word_id = p.leading_word_id
                JOIN words w2 ON w2.word_id = p.following_word_id
                WHERE w1.word_text = ?
                ORDER BY p.follow_count DESC
                LIMIT ?
                """;
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

    private long upsertWord(Connection conn, String wordText, boolean start, boolean end)
            throws SQLException {
        Optional<Long> existing = findWordId(conn, wordText);
        if (existing.isPresent()) {
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

    private void ensureWordExists(Connection conn, String wordText, boolean start, boolean end)
            throws SQLException {
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

    private void upsertPair(Connection conn, long leadingId, long followingId) throws SQLException {
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

    private ImportedFile insertImportedFile(Connection conn, String name, String path,
                                            int wordCount, int uniqueCount) throws SQLException {
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
            return new ImportedFile(id, name, path, wordCount, uniqueCount, LocalDateTime.now());
        }
    }

    private GeneratedSentence insertSentence(Connection conn, String text, String algorithm,
                                             String startWord, boolean duplicate)
            throws SQLException {
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
            return new GeneratedSentence(id, text, algorithm, startWord, LocalDateTime.now(), duplicate);
        }
    }

    private boolean sentenceExists(Connection conn, String text) throws SQLException {
        String sql = "SELECT 1 FROM generated_sentences WHERE LOWER(sentence_text) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void feedSentenceIntoCorpus(Connection conn, String sentence) throws SQLException {
        List<String> tokens = TextTokenizer.tokenize(sentence);
        boolean atStart = true;
        String previous = null;
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

    private void recordAutocompleteEvent(Connection conn, long leadId, long followId,
                                         String lead, String follow, boolean unknown)
            throws SQLException {
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

    private Optional<Long> findWordId(Connection conn, String text) throws SQLException {
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

    private Optional<Word> findWord(Connection conn, String text) throws SQLException {
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

    private List<String> listWordTexts(Connection conn, int limit) throws SQLException {
        String sql = "SELECT word_text FROM words ORDER BY frequency DESC LIMIT ?";
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

    private List<String> listSentenceStartWords(Connection conn) throws SQLException {
        String sql = "SELECT word_text FROM words WHERE is_sentence_start = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        }
    }

    private static Word mapWord(ResultSet rs) throws SQLException {
        return new Word(
                rs.getLong("word_id"),
                rs.getString("word_text"),
                rs.getInt("frequency"),
                rs.getBoolean("is_sentence_start"),
                rs.getBoolean("is_sentence_end"));
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
