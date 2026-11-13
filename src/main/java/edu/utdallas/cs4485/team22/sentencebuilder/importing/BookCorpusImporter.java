package edu.utdallas.cs4485.team22.sentencebuilder.importing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;
import java.util.stream.Stream;

/**
 * Imports a plain-text / Project Gutenberg book into the corpus tables.
 *
 * <p>Core text-processing and batch-insert logic from Ahmed Sherwani's
 * {@code BookImporter}, adapted to Umair Malik's schema
 * ({@code word}, {@code follows}, {@code imported_file}, {@code sentence}).</p>
 *
 * @author Ahmed Sherwani
 * @author Nirav Rohra
 * @date 2026-10-17
 */
public class BookCorpusImporter {

    private final Map<String, Integer> wordFrequency = new HashMap<>();
    private final Map<String, Integer> startCount = new HashMap<>();
    private final Map<String, Integer> endCount = new HashMap<>();
    private final Map<String, Integer> wordTransitions = new HashMap<>();
    private final AtomicBoolean cancelFlag;

    /**
     * @param cancelFlag shared cancel signal from the data service
     * @author Ahmed Sherwani
     * @date 2026-10-25
     */
    public BookCorpusImporter(AtomicBoolean cancelFlag) {
        this.cancelFlag = cancelFlag;
    }

    /**
     * Result of a successful import.
     *
     * @author Nirav Rohra
     * @date 2026-11-13
     */
    public record ImportResult(long fileId, String fileName, String filePath,
                               String bookTitle, int wordCount, int uniqueWordCount) {
    }

    /**
     * Reads, tokenizes, and inserts a book file into MySQL.
     *
     * @param connection       open JDBC connection (caller owns commit)
     * @param path             text file path
     * @param progressCallback optional 0..1 progress
     * @return import summary
     * @throws Exception on I/O, cancel, or SQL failure
     * @author Ahmed Sherwani
     * @date 2026-10-25
     */
    public ImportResult importBook(Connection connection, Path path,
                                   DoubleConsumer progressCallback) throws Exception {
        wordFrequency.clear();
        startCount.clear();
        endCount.clear();
        wordTransitions.clear();

        report(progressCallback, 0.02);
        checkCancel();

        String text = Files.readString(path);
        report(progressCallback, 0.15);
        checkCancel();

        // Prefer Gutenberg body markers when present; otherwise use full text.
        int startIndex = 0;
        int endIndex = text.length();
        String aliceStart = "Alice was beginning to get very tired of sitting";
        String endMarker = "THE END";
        int aliceIdx = text.indexOf(aliceStart);
        int endIdx = text.indexOf(endMarker);
        if (aliceIdx >= 0 && endIdx > aliceIdx) {
            startIndex = aliceIdx;
            endIndex = endIdx;
        }

        String[] sentences = extractSentences(text, startIndex, endIndex);
        String[] words = extractWords(text, startIndex, endIndex);
        report(progressCallback, 0.35);
        checkCancel();

        calculateStatistics(sentences);
        report(progressCallback, 0.50);
        checkCancel();

        String bookTitle = guessBookTitle(path.getFileName().toString(), text);
        long fileId = insertBookFile(connection, path.getFileName().toString(),
                path.toAbsolutePath().toString(), bookTitle, words.length, wordFrequency.size());
        report(progressCallback, 0.60);

        insertWords(connection, wordFrequency, startCount, endCount);
        report(progressCallback, 0.75);
        checkCancel();

        insertBookSentences(connection, sentences, fileId);
        report(progressCallback, 0.85);

        insertWordTransitions(connection, wordTransitions);
        report(progressCallback, 0.95);

        insertFileWords(connection, fileId);
        report(progressCallback, 1.0);

        return new ImportResult(fileId, path.getFileName().toString(),
                path.toAbsolutePath().toString(), bookTitle, words.length, wordFrequency.size());
    }

    /**
     * Tokenizes text into normalized words (Ahmed).
     *
     * @author Ahmed Sherwani
     * @date 2026-10-18
     */
    public String[] tokenizeWords(String text) {
        String normalizedText = text.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replace('\u2018', '\'');
        return Arrays.stream(normalizedText.split("[^\\p{L}\\p{N}']+"))
                .map(word -> word.replaceAll("^'+|'+$", ""))
                .filter(word -> !word.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * @author Ahmed Sherwani
     * @date 2026-10-27
     */
    public String[] extractSentences(String text, int startIndex, int endIndex) {
        String relevantText = text.substring(startIndex, endIndex).trim();
        relevantText = relevantText
                .replaceAll("(?<!\\w)_(?=\\w)", "")
                .replaceAll("(?<=\\w)_(?!\\w)", "");
        return relevantText.split("(?<=[.!?][\u201D\"'])\\s+|(?<=[.!?])\\s+");
    }

    /**
     * @author Ahmed Sherwani
     * @date 2026-10-19
     */
    public String[] extractWords(String text, int startIndex, int endIndex) {
        String relevantText = text.substring(startIndex, endIndex).trim();
        relevantText = relevantText
                .replaceAll("(?<!\\w)_(?=\\w)", "")
                .replaceAll("(?<=\\w)_(?!\\w)", "");
        return tokenizeWords(relevantText);
    }

    /**
     * Builds frequency, start/end, and transition maps (Ahmed).
     *
     * @author Ahmed Sherwani
     * @date 2026-10-20
     */
    public void calculateStatistics(String[] sentences) {
        wordFrequency.clear();
        startCount.clear();
        endCount.clear();
        wordTransitions.clear();

        for (String sentence : sentences) {
            String[] words = tokenizeWords(sentence);
            if (words.length == 0) {
                continue;
            }
            for (String word : words) {
                wordFrequency.merge(word, 1, Integer::sum);
            }
            startCount.merge(words[0], 1, Integer::sum);
            endCount.merge(words[words.length - 1], 1, Integer::sum);
            for (int i = 0; i < words.length - 1; i++) {
                String transition = words[i] + " " + words[i + 1];
                wordTransitions.merge(transition, 1, Integer::sum);
            }
        }
    }

    /**
     * Inserts/updates the {@code word} table (adapted from Ahmed to Umair columns).
     *
     * @author Ahmed Sherwani
     * @date 2026-10-22
     */
    public void insertWords(Connection connection,
                            Map<String, Integer> frequencies,
                            Map<String, Integer> starts,
                            Map<String, Integer> ends) throws SQLException {
        String sql = """
                INSERT INTO word (word_text, total_count, start_count, end_count)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  total_count = total_count + VALUES(total_count),
                  start_count = start_count + VALUES(start_count),
                  end_count = end_count + VALUES(end_count)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String word : frequencies.keySet()) {
                statement.setString(1, word);
                statement.setInt(2, frequencies.get(word));
                statement.setInt(3, starts.getOrDefault(word, 0));
                statement.setInt(4, ends.getOrDefault(word, 0));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Inserts {@code follows} rows (Ahmed transitions → Umair follows).
     *
     * @author Ahmed Sherwani
     * @date 2026-10-22
     */
    public void insertWordTransitions(Connection connection,
                                      Map<String, Integer> transitions) throws SQLException {
        Map<String, Integer> wordIds = loadWordIds(connection);
        String sql = """
                INSERT INTO follows (first_word_id, next_word_id, follow_count)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE follow_count = follow_count + VALUES(follow_count)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : transitions.entrySet()) {
                String[] pair = entry.getKey().split(" ", 2);
                if (pair.length != 2) {
                    throw new SQLException("Invalid word transition pair: " + entry.getKey());
                }
                Integer previousWordId = wordIds.get(pair[0]);
                Integer nextWordId = wordIds.get(pair[1]);
                if (previousWordId == null || nextWordId == null) {
                    throw new SQLException("Missing word ID for transition: " + entry.getKey());
                }
                statement.setInt(1, previousWordId);
                statement.setInt(2, nextWordId);
                statement.setInt(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Inserts an {@code imported_file} row.
     *
     * @author Ahmed Sherwani
     * @date 2026-10-24
     */
    public long insertBookFile(Connection connection, String fileName, String filePath,
                               String bookTitle, int totalWords, int uniqueWords)
            throws SQLException {
        String sql = """
                INSERT INTO imported_file
                  (file_name, file_path, book_title, word_count, unique_word_count)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fileName);
            statement.setString(2, filePath);
            statement.setString(3, bookTitle);
            statement.setInt(4, totalWords);
            statement.setInt(5, uniqueWords);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Could not retrieve generated file ID.");
    }

    /**
     * Stores extracted book sentences as BOOK-type rows.
     *
     * @author Ahmed Sherwani
     * @date 2026-10-24
     */
    public void insertBookSentences(Connection connection, String[] sentenceText, long fileId)
            throws SQLException {
        String sql = """
                INSERT INTO sentence (sentence_text, start_word_id, sentence_type)
                VALUES (?, NULL, 'BOOK')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int batch = 0;
            for (String sentence : sentenceText) {
                if (sentence == null || sentence.isBlank()) {
                    continue;
                }
                statement.setString(1, sentence.trim());
                statement.addBatch();
                batch++;
                if (batch % 500 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private void insertFileWords(Connection connection, long fileId) throws SQLException {
        Map<String, Integer> wordIds = loadWordIds(connection);
        String sql = """
                INSERT INTO file_word (file_id, word_id, count_in_file)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE count_in_file = count_in_file + VALUES(count_in_file)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
                Integer wordId = wordIds.get(entry.getKey());
                if (wordId == null) {
                    continue;
                }
                statement.setLong(1, fileId);
                statement.setInt(2, wordId);
                statement.setInt(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Map<String, Integer> loadWordIds(Connection connection) throws SQLException {
        Map<String, Integer> wordIds = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT word_id, word_text FROM word");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                wordIds.put(resultSet.getString("word_text"), resultSet.getInt("word_id"));
            }
        }
        return wordIds;
    }

    private String guessBookTitle(String fileName, String text) {
        if (text.contains("Alice was beginning to get very tired")) {
            return "Alice's Adventures in Wonderland";
        }
        try (Stream<String> lines = text.lines()) {
            return lines.map(String::trim)
                    .filter(l -> !l.isEmpty() && l.length() < 120)
                    .findFirst()
                    .orElse(fileName);
        }
    }

    /**
     * @return word frequency map from the last {@link #calculateStatistics} call
     * @author Ahmed Sherwani
     * @date 2026-10-20
     */
    public Map<String, Integer> getWordFrequency() {
        return Map.copyOf(wordFrequency);
    }

    /**
     * @return sentence-start counts from the last statistics pass
     * @author Ahmed Sherwani
     * @date 2026-10-20
     */
    public Map<String, Integer> getStartCount() {
        return Map.copyOf(startCount);
    }

    /**
     * @return sentence-end counts from the last statistics pass
     * @author Ahmed Sherwani
     * @date 2026-10-20
     */
    public Map<String, Integer> getEndCount() {
        return Map.copyOf(endCount);
    }

    /**
     * @return word-transition counts from the last statistics pass
     * @author Ahmed Sherwani
     * @date 2026-10-20
     */
    public Map<String, Integer> getWordTransitions() {
        return Map.copyOf(wordTransitions);
    }

    private void checkCancel() throws InterruptedException {
        if (cancelFlag != null && cancelFlag.get()) {
            throw new InterruptedException("Import cancelled");
        }
    }

    private static void report(DoubleConsumer callback, double value) {
        if (callback != null) {
            callback.accept(Math.min(1.0, Math.max(0.0, value)));
        }
    }
}
