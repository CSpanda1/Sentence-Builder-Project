package edu.utdallas.cs4485.team22.sentencebuilder.service;

import edu.utdallas.cs4485.team22.sentencebuilder.model.GeneratedSentence;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.model.ImportedFile;
import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;

/**
 * In-memory stub of {@link DataService} so the WP7 UI can run without MySQL.
 *
 * <p>Seeded with a small demo corpus. Import, generation, and autocomplete
 * mutate this state so every screen is demoable end-to-end.</p>
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class StubDataService implements DataService {

    private final AtomicLong wordIdSeq = new AtomicLong(1);
    private final AtomicLong fileIdSeq = new AtomicLong(1);
    private final AtomicLong sentenceIdSeq = new AtomicLong(1);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private final Map<String, Word> wordsByText = new LinkedHashMap<>();
    private final Map<String, List<WordPair>> followers = new HashMap<>();
    private final List<ImportedFile> importedFiles = new ArrayList<>();
    private final List<GeneratedSentence> sentences = new ArrayList<>();
    private final Set<String> sentenceTexts = new HashSet<>();
    private final Random random = new Random(42);

    private int totalTokens;
    private String lastImportName;

    /**
     * Seeds a small demo corpus so screens are useful before any import.
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public StubDataService() {
        seedDemoCorpus();
    }

    /**
     * Loads sample words and follower pairs for UI demos.
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    private void seedDemoCorpus() {
        String sample = "The quick brown fox jumps over the lazy dog. "
                + "The fox was quick and the dog was lazy. "
                + "A brown fox jumps again over a lazy dog. "
                + "Once upon a time the fox met the dog.";
        ingestTokens(tokenize(sample), null);
        lastImportName = "(demo corpus)";
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception {
        cancelRequested.set(false);
        String content = Files.readString(path);
        List<String> tokens = tokenize(content);

        int size = Math.max(tokens.size(), 1);
        for (int i = 0; i < tokens.size(); i++) {
            if (cancelRequested.get()) {
                throw new InterruptedException("Import cancelled");
            }
            // Simulate work so the progress bar is visible.
            Thread.sleep(8);
            if (progressCallback != null) {
                progressCallback.accept((i + 1) / (double) size);
            }
        }

        int beforeUnique = wordsByText.size();
        ingestTokens(tokens, path.getFileName().toString());
        int uniqueAdded = wordsByText.size() - beforeUnique;

        ImportedFile record = new ImportedFile(
                fileIdSeq.getAndIncrement(),
                path.getFileName().toString(),
                path.toAbsolutePath().toString(),
                tokens.size(),
                Math.max(uniqueAdded, countUnique(tokens)),
                LocalDateTime.now());
        importedFiles.add(0, record);
        lastImportName = record.getFileName();
        if (progressCallback != null) {
            progressCallback.accept(1.0);
        }
        return record;
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public void cancelImport() {
        cancelRequested.set(true);
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength) {
        String seed = normalize(startWord);
        if (seed.isEmpty()) {
            throw new IllegalArgumentException("Start word is required");
        }
        ensureWord(seed, false, false);

        List<String> parts = new ArrayList<>();
        parts.add(capitalize(seed));
        String current = seed;
        int limit = Math.max(2, Math.min(maxLength, 40));

        for (int i = 1; i < limit; i++) {
            Optional<String> next = pickFollower(current, algorithm);
            if (next.isEmpty()) {
                break;
            }
            String follower = next.get();
            parts.add(follower);
            current = follower;
            Word w = wordsByText.get(follower);
            if (w != null && w.isSentenceEnd() && i > 2) {
                break;
            }
        }

        String text = String.join(" ", parts);
        if (!text.endsWith(".")) {
            text = text + ".";
        }
        return storeSentence(text, algorithm.getDisplayName(), seed);
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength) {
        List<String> starts = wordsByText.values().stream()
                .filter(Word::isSentenceStart)
                .map(Word::getText)
                .collect(Collectors.toCollection(ArrayList::new));
        if (starts.isEmpty()) {
            starts.addAll(wordsByText.keySet());
        }
        List<GeneratedSentence> batch = new ArrayList<>();
        int n = Math.max(1, Math.min(count, 50));
        for (int i = 0; i < n; i++) {
            String start = starts.get(random.nextInt(starts.size()));
            batch.add(generate(start, algorithm, maxLength));
        }
        return batch;
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public List<WordPair> suggestNext(String leadingWord, int limit) {
        String key = normalize(leadingWord);
        List<WordPair> pairs = followers.getOrDefault(key, List.of());
        return pairs.stream()
                .sorted(Comparator.comparingInt(WordPair::getCount).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public Word addOrIncrementWord(String wordText) {
        String key = normalize(wordText);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        Word existing = wordsByText.get(key);
        if (existing != null) {
            existing.setFrequency(existing.getFrequency() + 1);
            totalTokens++;
            return existing;
        }
        Word created = new Word(wordIdSeq.getAndIncrement(), key, 1, false, false);
        wordsByText.put(key, created);
        totalTokens++;
        return created;
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public void recordFollow(String leadingWord, String followingWord) {
        String lead = normalize(leadingWord);
        String follow = normalize(followingWord);
        if (lead.isEmpty() || follow.isEmpty()) {
            return;
        }
        ensureWord(lead, false, false);
        ensureWord(follow, false, false);
        List<WordPair> list = followers.computeIfAbsent(lead, k -> new ArrayList<>());
        for (WordPair pair : list) {
            if (pair.getFollowingWord().equals(follow)) {
                pair.incrementCount();
                return;
            }
        }
        list.add(new WordPair(lead, follow, 1));
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public List<Word> listWords(String filter) {
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        return wordsByText.values().stream()
                .filter(w -> f.isEmpty() || w.getText().contains(f))
                .sorted(Comparator.comparing(Word::getText))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public boolean updateWord(Word word) {
        String key = normalize(word.getText());
        Word existing = wordsByText.values().stream()
                .filter(w -> w.getId() == word.getId())
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return false;
        }
        if (!existing.getText().equals(key)) {
            wordsByText.remove(existing.getText());
            existing.setText(key);
            wordsByText.put(key, existing);
        }
        existing.setFrequency(Math.max(1, word.getFrequency()));
        existing.setSentenceStart(word.isSentenceStart());
        existing.setSentenceEnd(word.isSentenceEnd());
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public Optional<Word> findWord(String text) {
        return Optional.ofNullable(wordsByText.get(normalize(text)));
    }

    @Override
    public List<ImportedFile> listImportedFiles() {
        return List.copyOf(importedFiles);
    }

    @Override
    public List<GeneratedSentence> listSentences() {
        return List.copyOf(sentences);
    }

    @Override
    public int getTotalWordCount() {
        return totalTokens;
    }

    @Override
    public int getUniqueWordCount() {
        return wordsByText.size();
    }

    @Override
    public Optional<String> getLastImportName() {
        return Optional.ofNullable(lastImportName);
    }

    // ---- helpers ----

    private GeneratedSentence storeSentence(String text, String algorithm, String startWord) {
        boolean duplicate = !sentenceTexts.add(text.toLowerCase(Locale.ROOT));
        GeneratedSentence gs = new GeneratedSentence(
                sentenceIdSeq.getAndIncrement(),
                text,
                algorithm,
                startWord,
                LocalDateTime.now(),
                duplicate);
        sentences.add(0, gs);
        // Feed generated text back into the corpus (batch feedback per plan).
        ingestTokens(tokenize(text), null);
        return gs;
    }

    private Optional<String> pickFollower(String current, GenerationAlgorithm algorithm) {
        List<WordPair> pairs = followers.get(current);
        if (pairs == null || pairs.isEmpty()) {
            List<String> all = new ArrayList<>(wordsByText.keySet());
            if (all.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(all.get(random.nextInt(all.size())));
        }
        return switch (algorithm) {
            case MOST_FREQUENT -> pairs.stream()
                    .max(Comparator.comparingInt(WordPair::getCount))
                    .map(WordPair::getFollowingWord);
            case PROBABILITY_WEIGHTED -> Optional.of(weightedPick(pairs));
            case RANDOM_WALK -> Optional.of(
                    pairs.get(random.nextInt(pairs.size())).getFollowingWord());
        };
    }

    private String weightedPick(List<WordPair> pairs) {
        int total = pairs.stream().mapToInt(WordPair::getCount).sum();
        int roll = random.nextInt(Math.max(total, 1));
        int running = 0;
        for (WordPair pair : pairs) {
            running += pair.getCount();
            if (roll < running) {
                return pair.getFollowingWord();
            }
        }
        return pairs.get(pairs.size() - 1).getFollowingWord();
    }

    private void ingestTokens(List<String> tokens, String sourceLabel) {
        if (tokens.isEmpty()) {
            return;
        }
        boolean atStart = true;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            boolean end = token.endsWith(".");
            String word = normalize(token.replaceAll("[.]$", ""));
            if (word.isEmpty()) {
                continue;
            }
            boolean existed = wordsByText.containsKey(word);
            Word w = ensureWord(word, atStart, end);
            if (existed) {
                w.setFrequency(w.getFrequency() + 1);
            }
            totalTokens++;
            if (i > 0) {
                String prev = normalize(tokens.get(i - 1).replaceAll("[.]$", ""));
                if (!prev.isEmpty()) {
                    recordFollowInternal(prev, word);
                }
            }
            atStart = end;
        }
    }

    private void recordFollowInternal(String lead, String follow) {
        List<WordPair> list = followers.computeIfAbsent(lead, k -> new ArrayList<>());
        for (WordPair pair : list) {
            if (pair.getFollowingWord().equals(follow)) {
                pair.incrementCount();
                return;
            }
        }
        list.add(new WordPair(lead, follow, 1));
    }

    private Word ensureWord(String text, boolean start, boolean end) {
        Word existing = wordsByText.get(text);
        if (existing != null) {
            if (start) {
                existing.setSentenceStart(true);
            }
            if (end) {
                existing.setSentenceEnd(true);
            }
            return existing;
        }
        Word created = new Word(wordIdSeq.getAndIncrement(), text, 1, start, end);
        wordsByText.put(text, created);
        return created;
    }

    private static List<String> tokenize(String content) {
        String cleaned = content.replaceAll("[^A-Za-z0-9.'\\s]", " ");
        String[] raw = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String r : raw) {
            if (!r.isBlank()) {
                tokens.add(r.trim());
            }
        }
        return tokens;
    }

    private static int countUnique(List<String> tokens) {
        Set<String> set = new HashSet<>();
        for (String t : tokens) {
            String n = normalize(t.replaceAll("[.]$", ""));
            if (!n.isEmpty()) {
                set.add(n);
            }
        }
        return set.size();
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
