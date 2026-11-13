package edu.utdallas.cs4485.team22.sentencebuilder.service;

import edu.utdallas.cs4485.team22.sentencebuilder.autocomplete.AutoCompleteRecorder;
import edu.utdallas.cs4485.team22.sentencebuilder.db.DatabaseConnection;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.Follower;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.FollowerProvider;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.GeneratorFactory;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.SentenceGenerator;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.WordNormalizer;
import edu.utdallas.cs4485.team22.sentencebuilder.importing.BookCorpusImporter;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GeneratedSentence;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.model.ImportedFile;
import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

/**
 * MySQL JDBC data layer implementing Umair's schema and wiring Ahmed's importer
 * plus Umair's generation / autocomplete contracts.
 *
 * @author Nirav Rohra
 * @author Ahmed Sherwani
 * @author Umair Malik
 * @date 2026-11-13
 */
public class JdbcDataService implements DataService, FollowerProvider, AutoCompleteRecorder {

    private final DatabaseConnection database;
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final Random random = new Random();

    public JdbcDataService(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception {
        cancelRequested.set(false);
        BookCorpusImporter importer = new BookCorpusImporter(cancelRequested);
        try (Connection conn = database.open()) {
            conn.setAutoCommit(false);
            try {
                BookCorpusImporter.ImportResult result =
                        importer.importBook(conn, path, progressCallback);
                conn.commit();
                return new ImportedFile(
                        result.fileId(),
                        result.fileName(),
                        result.filePath(),
                        result.wordCount(),
                        result.uniqueWordCount(),
                        LocalDateTime.now());
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void cancelImport() {
        cancelRequested.set(true);
    }

    @Override
    public GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength) {
        String seed = WordNormalizer.normalize(startWord);
        if (seed.isEmpty()) {
            throw new IllegalArgumentException("Start word is required");
        }
        SentenceGenerator generator = GeneratorFactory.create(algorithm, this, random);
        String text = generator.generate(seed, maxLength);
        try (Connection conn = database.open()) {
            conn.setAutoCommit(false);
            try {
                ensureWord(conn, seed, 0, 1, 0);
                Integer startId = findWordId(conn, seed).map(Long::intValue).orElse(null);
                boolean duplicate = sentenceTextExists(conn, text);
                long sentenceId = insertSentence(conn, text, startId, "GENERATED");
                insertGeneratedDetail(conn, sentenceId, generator.getName(), duplicate, true);
                // Feed back into corpus (WP6).
                feedSentenceIntoCorpus(conn, text);
                updateGeneratedAddedFlag(conn, sentenceId, true);
                conn.commit();
                return new GeneratedSentence(
                        sentenceId, text, generator.getName(), seed,
                        LocalDateTime.now(), duplicate);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength) {
        SentenceGenerator generator = GeneratorFactory.create(algorithm, this, random);
        List<String> texts = generator.generateBatch(count, maxLength);
        List<GeneratedSentence> stored = new ArrayList<>();
        for (String text : texts) {
            String start = text.isEmpty() ? "" : text.split("\\s+")[0].replace(".", "").toLowerCase();
            try (Connection conn = database.open()) {
                conn.setAutoCommit(false);
                try {
                    Integer startId = findWordId(conn, WordNormalizer.normalize(start))
                            .map(Long::intValue).orElse(null);
                    boolean duplicate = sentenceTextExists(conn, text);
                    long sentenceId = insertSentence(conn, text, startId, "GENERATED");
                    insertGeneratedDetail(conn, sentenceId, generator.getName(), duplicate, true);
                    feedSentenceIntoCorpus(conn, text);
                    conn.commit();
                    stored.add(new GeneratedSentence(
                            sentenceId, text, generator.getName(), start,
                            LocalDateTime.now(), duplicate));
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Batch generation failed: " + e.getMessage(), e);
            }
        }
        return stored;
    }

    @Override
    public List<WordPair> suggestNext(String leadingWord, int limit) {
        return getFollowers(WordNormalizer.normalize(leadingWord)).stream()
                .limit(Math.max(1, limit))
                .map(f -> new WordPair(leadingWord, f.getWordText(), f.getFollowCount()))
                .toList();
    }

    @Override
    public Word addOrIncrementWord(String wordText) {
        String key = WordNormalizer.normalize(wordText);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        try (Connection conn = database.open()) {
            ensureWord(conn, key, 1, 0, 0);
            return findWord(conn, key).orElseThrow();
        } catch (SQLException e) {
            throw new IllegalStateException("Add word failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void recordFollow(String leadingWord, String followingWord) {
        incrementFollowCount(leadingWord, followingWord);
    }

    @Override
    public void addWordIfMissing(String wordText) {
        String key = WordNormalizer.normalize(wordText);
        if (key.isEmpty()) {
            return;
        }
        try (Connection conn = database.open()) {
            if (findWordId(conn, key).isEmpty()) {
                ensureWord(conn, key, 1, 0, 0);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("addWordIfMissing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void incrementFollowCount(String firstWord, String nextWord) {
        String lead = WordNormalizer.normalize(firstWord);
        String follow = WordNormalizer.normalize(nextWord);
        if (lead.isEmpty() || follow.isEmpty()) {
            return;
        }
        try (Connection conn = database.open()) {
            long leadId = ensureWord(conn, lead, 0, 0, 0);
            long followId = ensureWord(conn, follow, 0, 0, 0);
            String sql = """
                    INSERT INTO follows (first_word_id, next_word_id, follow_count)
                    VALUES (?, ?, 1)
                    ON DUPLICATE KEY UPDATE follow_count = follow_count + 1
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, leadId);
                ps.setLong(2, followId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("incrementFollowCount failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Follower> getFollowers(String wordText) {
        String sql = """
                SELECT w2.word_text, f.follow_count
                FROM follows f
                JOIN word w1 ON w1.word_id = f.first_word_id
                JOIN word w2 ON w2.word_id = f.next_word_id
                WHERE w1.word_text = ?
                ORDER BY f.follow_count DESC
                """;
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, WordNormalizer.normalize(wordText));
            try (ResultSet rs = ps.executeQuery()) {
                List<Follower> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Follower(rs.getString(1), rs.getInt(2)));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("getFollowers failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTotalCount(String wordText) {
        return scalarInt("SELECT total_count FROM word WHERE word_text = ?",
                WordNormalizer.normalize(wordText));
    }

    @Override
    public int getEndCount(String wordText) {
        return scalarInt("SELECT end_count FROM word WHERE word_text = ?",
                WordNormalizer.normalize(wordText));
    }

    @Override
    public String getRandomStartWord() {
        String sql = """
                SELECT word_text FROM word
                WHERE start_count > 0
                ORDER BY RAND() LIMIT 1
                """;
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
            // Fall back to any word.
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT word_text FROM word ORDER BY RAND() LIMIT 1");
                 ResultSet rs2 = ps2.executeQuery()) {
                return rs2.next() ? rs2.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("getRandomStartWord failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Word> listWords(String filter) {
        String f = filter == null ? "" : filter.trim().toLowerCase();
        String sql = f.isEmpty()
                ? "SELECT word_id, word_text, total_count, start_count, end_count FROM word ORDER BY word_text"
                : "SELECT word_id, word_text, total_count, start_count, end_count FROM word WHERE word_text LIKE ? ORDER BY word_text";
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
            throw new IllegalStateException("listWords failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateWord(Word word) {
        String sql = """
                UPDATE word
                SET word_text = ?, total_count = ?,
                    start_count = ?, end_count = ?
                WHERE word_id = ?
                """;
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, WordNormalizer.normalize(word.getText()));
            ps.setInt(2, Math.max(1, word.getFrequency()));
            ps.setInt(3, word.isSentenceStart() ? Math.max(1, word.getStartCount()) : 0);
            ps.setInt(4, word.isSentenceEnd() ? Math.max(1, word.getEndCount()) : 0);
            ps.setLong(5, word.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("updateWord failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Word> findWord(String text) {
        try (Connection conn = database.open()) {
            return findWord(conn, WordNormalizer.normalize(text));
        } catch (SQLException e) {
            throw new IllegalStateException("findWord failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ImportedFile> listImportedFiles() {
        String sql = """
                SELECT file_id, file_name, COALESCE(file_path, ''), word_count,
                       unique_word_count, import_time
                FROM imported_file
                ORDER BY import_time DESC
                """;
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ImportedFile> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new ImportedFile(
                        rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4),
                        rs.getInt(5),
                        rs.getTimestamp(6).toLocalDateTime()));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("listImportedFiles failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeneratedSentence> listSentences() {
        String sql = """
                SELECT s.sentence_id, s.sentence_text, COALESCE(g.algorithm, 'BOOK'),
                       COALESCE(w.word_text, ''), s.created_at, COALESCE(g.is_duplicate, FALSE)
                FROM sentence s
                LEFT JOIN generated_sentence g ON g.sentence_id = s.sentence_id
                LEFT JOIN word w ON w.word_id = s.start_word_id
                WHERE s.sentence_type IN ('GENERATED', 'TYPED')
                ORDER BY s.created_at DESC
                """;
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<GeneratedSentence> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new GeneratedSentence(
                        rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getTimestamp(5).toLocalDateTime(),
                        rs.getBoolean(6)));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("listSentences failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getTotalWordCount() {
        return scalarInt("SELECT COALESCE(SUM(total_count), 0) FROM word", null);
    }

    @Override
    public int getUniqueWordCount() {
        return scalarInt("SELECT COUNT(*) FROM word", null);
    }

    @Override
    public Optional<String> getLastImportName() {
        String sql = "SELECT file_name FROM imported_file ORDER BY import_time DESC LIMIT 1";
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("getLastImportName failed: " + e.getMessage(), e);
        }
    }

    // ---- helpers ----

    private void feedSentenceIntoCorpus(Connection conn, String sentence) throws SQLException {
        BookCorpusImporter importer = new BookCorpusImporter(cancelRequested);
        String[] sentences = {sentence};
        importer.calculateStatistics(sentences);
        importer.insertWords(conn,
                importer.getWordFrequency(),
                importer.getStartCount(),
                importer.getEndCount());
        importer.insertWordTransitions(conn, importer.getWordTransitions());
    }

    private long ensureWord(Connection conn, String text, int addTotal, int addStart, int addEnd)
            throws SQLException {
        Optional<Long> existing = findWordId(conn, text);
        if (existing.isPresent()) {
            if (addTotal > 0 || addStart > 0 || addEnd > 0) {
                String sql = """
                        UPDATE word SET
                          total_count = total_count + ?,
                          start_count = start_count + ?,
                          end_count = end_count + ?
                        WHERE word_id = ?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, addTotal);
                    ps.setInt(2, addStart);
                    ps.setInt(3, addEnd);
                    ps.setLong(4, existing.get());
                    ps.executeUpdate();
                }
            }
            return existing.get();
        }
        String sql = """
                INSERT INTO word (word_text, total_count, start_count, end_count)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, text);
            ps.setInt(2, Math.max(addTotal, 1));
            ps.setInt(3, addStart);
            ps.setInt(4, addEnd);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private Optional<Long> findWordId(Connection conn, String text) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT word_id FROM word WHERE word_text = ?")) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }
        }
    }

    private Optional<Word> findWord(Connection conn, String text) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT word_id, word_text, total_count, start_count, end_count FROM word WHERE word_text = ?")) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapWord(rs)) : Optional.empty();
            }
        }
    }

    private boolean sentenceTextExists(Connection conn, String text) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sentence WHERE LOWER(sentence_text) = LOWER(?) LIMIT 1")) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long insertSentence(Connection conn, String text, Integer startWordId, String type)
            throws SQLException {
        String sql = "INSERT INTO sentence (sentence_text, start_word_id, sentence_type) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, text);
            if (startWordId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, startWordId);
            }
            ps.setString(3, type);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertGeneratedDetail(Connection conn, long sentenceId, String algorithm,
                                       boolean duplicate, boolean added) throws SQLException {
        String sql = """
                INSERT INTO generated_sentence (sentence_id, algorithm, is_duplicate, added_to_corpus)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sentenceId);
            ps.setString(2, algorithm);
            ps.setBoolean(3, duplicate);
            ps.setBoolean(4, added);
            ps.executeUpdate();
        }
    }

    private void updateGeneratedAddedFlag(Connection conn, long sentenceId, boolean added)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE generated_sentence SET added_to_corpus = ? WHERE sentence_id = ?")) {
            ps.setBoolean(1, added);
            ps.setLong(2, sentenceId);
            ps.executeUpdate();
        }
    }

    private Word mapWord(ResultSet rs) throws SQLException {
        int start = rs.getInt("start_count");
        int end = rs.getInt("end_count");
        Word w = new Word(
                rs.getLong("word_id"),
                rs.getString("word_text"),
                rs.getInt("total_count"),
                start > 0,
                end > 0);
        w.setStartCount(start);
        w.setEndCount(end);
        return w;
    }

    private int scalarInt(String sql, String param) {
        try (Connection conn = database.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
