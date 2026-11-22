package edu.utdallas.cs4485.team22.sentencebuilder.service;

/*****************************************************************************************************************
 * StubDataService.java
 *
 * Provides an in-memory implementation of DataService for running and
 * demonstrating the application without a MySQL database.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.generation.FollowerPicker;
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
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class StubDataService implements DataService {

    // Generates sequential IDs for words stored in memory.
    private final AtomicLong wordIdSeq = new AtomicLong(1);
    // Generates sequential IDs for imported-file records.
    private final AtomicLong fileIdSeq = new AtomicLong(1);
    // Generates sequential IDs for generated sentences.
    private final AtomicLong sentenceIdSeq = new AtomicLong(1);
    // Signals that an in-progress text import should stop.
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    // Stores corpus words by their normalized text for quick lookup.
    private final Map<String, Word> wordsByText = new LinkedHashMap<>();
    // Stores observed following-word pairs grouped by their leading word.
    private final Map<String, List<WordPair>> followers = new HashMap<>();
    // Keeps a history of imported files.
    private final List<ImportedFile> importedFiles = new ArrayList<>();
    // Keeps generated sentence records.
    private final List<GeneratedSentence> sentences = new ArrayList<>();
    // Tracks sentence text to detect duplicate generations.
    private final Set<String> sentenceTexts = new HashSet<>();
    // Provides repeatable random choices for demo behavior.
    private final Random random = new Random(42);
    // Chooses a following word according to the requested generation strategy.
    private final FollowerPicker picker = new FollowerPicker(random);

    // Tracks total word occurrences in the in-memory corpus.
    private int totalTokens;
    // Stores the name of the most recently imported file, if any.
    private String lastImportName;

    /**
     * Seeds the in-memory service with sample data for the demo screens.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Create a stub service that loads a demo corpus on construction so
     * the UI works before a file is imported.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    public StubDataService() {
        seedDemoCorpus();
    }

    /**
     * Loads sample sentences and records the demo corpus as the latest import.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Seed the word frequencies and follower counts from a few sample
     * sentences, then set a demo name for the status display.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void seedDemoCorpus() {
        // Use the same ingestion path as imported text to build demo statistics.
        String sample = "The quick brown fox jumps over the lazy dog. "
                + "The fox was quick and the dog was lazy. "
                + "A brown fox jumps again over a lazy dog. "
                + "Once upon a time the fox met the dog.";
        ingestTokens(tokenize(sample), null);
        lastImportName = "(demo corpus)";
    }

    /**
     * Imports a text file into the in-memory corpus, reports simulated progress,
     * and records import details. This stub implementation lets the UI's import
     * workflow run without a database.
     *
     * @param path path to the text file
     * @param progressCallback receives import progress when provided
     * @return the created import-history record
     * @throws Exception if the file cannot be read or the import is cancelled
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Read and tokenize a text file, report progress and honor
     * cancellation, then update the in-memory corpus and record the import.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception {
        // Clear any cancellation request from a previous import.
        cancelRequested.set(false);
        // Read the file and convert its contents into tokens.
        String content = Files.readString(path);
        List<String> tokens = tokenize(content);

        // Use at least one as the progress denominator to avoid division by zero.
        int size = Math.max(tokens.size(), 1);
        // Simulate import work and report progress for the UI.
        for (int i = 0; i < tokens.size(); i++) {
            // Stop the import if cancellation was requested.
            if (cancelRequested.get()) {
                throw new InterruptedException("Import cancelled");
            }
            // Simulate work so the progress bar is visible.
            Thread.sleep(8);
            if (progressCallback != null) {
                progressCallback.accept((i + 1) / (double) size);
            }
        }

        // Add the tokens to the in-memory corpus and calculate newly added words.
        int beforeUnique = wordsByText.size();
        ingestTokens(tokens, path.getFileName().toString());
        int uniqueAdded = wordsByText.size() - beforeUnique;

        // Create a history record with file details and token counts.
        ImportedFile record = new ImportedFile(
                fileIdSeq.getAndIncrement(),
                path.getFileName().toString(),
                path.toAbsolutePath().toString(),
                tokens.size(),
                Math.max(uniqueAdded, countUnique(tokens)),
                LocalDateTime.now());
        // Put the newest import first and update the latest-import display value.
        importedFiles.add(0, record);
        lastImportName = record.getFileName();
        // Report completion and return the import record.
        if (progressCallback != null) {
            progressCallback.accept(1.0);
        }
        return record;
    }

    /**
     * Requests cancellation of the current in-memory import. The import checks
     * this flag while processing tokens and stops when it is set.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Set the cancellation flag that the in-memory importer checks while
     * reporting progress.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public void cancelImport() {
        cancelRequested.set(true);
    }

    /**
     * Extends a starting phrase using in-memory word transitions and stores the result.
     *
     * @param startWord starting word or phrase
     * @param algorithm follower selection strategy
     * @param maxLength total word count, including the starting phrase
     * @return the stored sentence record
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Generate a sentence with the selected follower algorithm. Include
     * the seed phrase in the requested word count, begin with followers of its last word, and fall
     * back to corpus words as needed.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength) {
        // Normalize the starting phrase before validating the input.
        String prefix = normalize(startWord).replaceAll("(?U)^\\s+|\\s+$", "");
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("Enter a start word or phrase.");
        }

        // Count the starting phrase toward the requested total.
        List<String> prefixWords = List.of(prefix.split("(?U)\\s+"));
        int limit = Math.max(2, Math.min(maxLength, 40));
        if (prefixWords.size() > limit) {
            throw new IllegalArgumentException("The starting phrase has more words than the selected word count.");
        }
        // Keep the first word for history and use the last word for the first lookup.
        String seed = prefixWords.getFirst();
        String current = prefixWords.getLast();

        ensureWord(seed, false, false);
        List<String> parts = new ArrayList<>(prefixWords);
        parts.set(0, capitalize(seed));

        // Collect single-word fallback candidates for gaps in the transition data.
        List<String> fallback = wordsByText.keySet().stream()
                .filter(FollowerPicker::isSingleWord)
                .toList();
        // Choose and append one follower at a time until the requested count is reached.
        while (parts.size() < limit) {
            List<WordPair> pairs = followers.getOrDefault(current, List.of()).stream()
                    .filter(pair -> FollowerPicker.isSingleWord(pair.getFollowingWord()))
                    .toList();
            Optional<String> next = picker.pick(pairs, algorithm, fallback);
            if (next.isEmpty()) {
                break;
            }
            String follower = next.get();
            parts.add(follower);
            current = follower;
        }

        // Add a final period when needed before storing the sentence.
        String text = String.join(" ", parts);
        if (!text.endsWith(".")) {
            text = text + ".";
        }
        return storeSentence(text, algorithm.getDisplayName(), seed);
    }

    /**
     * Generates several sentences from randomly selected corpus words.
     *
     * @param count number of sentences requested
     * @param algorithm follower selection strategy
     * @param maxLength total word count per sentence
     * @return the generated sentence records
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Pick random single-word sentence starts, fall back to any single-
     * word corpus entry if needed, and reuse the single-sentence generator for a batch.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength) {
        // Prefer words observed at sentence starts, then fall back to the vocabulary.
        List<String> starts = wordsByText.values().stream()
                .filter(Word::isSentenceStart)
                .map(Word::getText)
                .filter(FollowerPicker::isSingleWord)
                .collect(Collectors.toCollection(ArrayList::new));
        if (starts.isEmpty()) {
            wordsByText.keySet().stream().filter(FollowerPicker::isSingleWord).forEach(starts::add);
        }
        if (starts.isEmpty()) {
            throw new IllegalStateException("Corpus is empty; import text first");
        }
        List<GeneratedSentence> batch = new ArrayList<>();
        int n = Math.max(1, Math.min(count, 50));
        // Reuse single-sentence generation for each randomly chosen start.
        for (int i = 0; i < n; i++) {
            String start = starts.get(random.nextInt(starts.size()));
            batch.add(generate(start, algorithm, maxLength));
        }
        return batch;
    }

    /**
     * Returns the most frequent recorded followers of a normalized word.
     *
     * @param leadingWord word whose followers are requested
     * @param limit maximum suggestions requested
     * @return follower pairs ordered by descending count
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Look up a leading word in the in-memory transition map and return
     * its highest-count followers up to the requested limit.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<WordPair> suggestNext(String leadingWord, int limit) {
        String key = normalize(leadingWord);
        // Rank observed followers by count before applying the suggestion limit.
        List<WordPair> pairs = followers.getOrDefault(key, List.of());
        return pairs.stream()
                .sorted(Comparator.comparingInt(WordPair::getCount).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /**
     * Adds a normalized word or increments its frequency and the corpus token total.
     *
     * @param wordText word text to add or count again
     * @return the stored word
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Reject blank input, then insert the normalized word at frequency one
     * or increment its existing frequency. Update the total token count in either case.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public Word addOrIncrementWord(String wordText) {
        String key = normalize(wordText);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        // An existing word gains one occurrence. A new word starts at frequency one.
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
     * Registers an observed transition between two normalized words.
     *
     * @param leadingWord preceding word
     * @param followingWord word observed immediately after it
     *
     * ChatGPT Codex was used to create this method
     * Prompt:Ensure both words exist in the corpus, then increment their
     * transition count or create a new pair.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public void recordFollow(String leadingWord, String followingWord) {
        String lead = normalize(leadingWord);
        String follow = normalize(followingWord);
        if (lead.isEmpty() || follow.isEmpty()) {
            return;
        }
        // Register both words before updating their transition count.
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
     * Returns corpus words in alphabetical order with optional substring filtering.
     *
     * @param filter optional substring to match
     * @return matching words sorted by text
     *
     * ChatGPT Codex was used to create this method
     * Prompt:Filter the in-memory vocabulary by a case-insensitive substring and
     * sort the matching words by text.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
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
     * Updates editable fields for an existing word identified by ID.
     *
     * @param word updated values and the ID of the stored word
     * @return whether the word was found and updated
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Find a word by ID, rekey the map if its text changes, update the
     * corpus total for the frequency change, and save its sentence-boundary flags.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public boolean updateWord(Word word) {
        String key = normalize(word.getText());
        // Match by ID so an edit can change the word text.
        Word existing = wordsByText.values().stream()
                .filter(w -> w.getId() == word.getId())
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return false;
        }
        // Move the map entry to the new key when the text changes.
        if (!existing.getText().equals(key)) {
            wordsByText.remove(existing.getText());
            existing.setText(key);
            wordsByText.put(key, existing);
        }
        // Apply the frequency difference to the corpus total.
        totalTokens += Math.max(1, word.getFrequency()) - existing.getFrequency();
        existing.setFrequency(Math.max(1, word.getFrequency()));
        existing.setSentenceStart(word.isSentenceStart());
        existing.setSentenceEnd(word.isSentenceEnd());
        return true;
    }

    /**
     * Looks up a normalized word in the in-memory corpus.
     *
     * @param text word text to look up
     * @return the matching word, or empty if absent
     *
     * ChatGPT Codex was used to create this method
     * Prompt:Normalize the supplied text and return the matching Word in an
     * Optional.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public Optional<Word> findWord(String text) {
        return Optional.ofNullable(wordsByText.get(normalize(text)));
    }

    /**
     * Returns an unmodifiable snapshot of the import history, newest first.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Return a copy of the in-memory import list so callers cannot modify
     * the stored history.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<ImportedFile> listImportedFiles() {
        return List.copyOf(importedFiles);
    }

    /**
     * Returns an unmodifiable snapshot of generated sentence history, newest first.
     *
     * ChatGPT Codex was used to create this method
     * Prompt:Return a copy of the in-memory generated sentence list for the
     * reports screen.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public List<GeneratedSentence> listSentences() {
        return List.copyOf(sentences);
    }

    /**
     * Returns the number of word occurrences tracked by the corpus.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Expose the running token total for the application status and
     * reports.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public int getTotalWordCount() {
        return totalTokens;
    }

    /**
     * Returns the number of distinct words in the corpus.
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Use the size of the word map to report the number of stored
     * vocabulary entries.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public int getUniqueWordCount() {
        return wordsByText.size();
    }

    /**
     * Returns the latest import name when one is available.
     *
     * ChatGPT Codex was used to create this method
     * Prompt:Expose the latest import name as an Optional for the application's
     * status display.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    @Override
    public Optional<String> getLastImportName() {
        return Optional.ofNullable(lastImportName);
    }

    // ---- helpers ----

    /**
     * Records generated output, marks duplicate text, and feeds the sentence back into the corpus.
     *
     * @param text generated sentence text
     * @param algorithm display name of the generation strategy
     * @param startWord first word of the starting phrase
     * @return the stored sentence record
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Check for duplicates without regard to case, create a sentence
     * record with its ID and timestamp, and feed its tokens back into the corpus after saving it.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private GeneratedSentence storeSentence(String text, String algorithm, String startWord) {
        // Track sentence text without regard to case for duplicate detection.
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

    /**
     * Updates the vocabulary, token count, sentence-boundary flags, and adjacent transitions.
     *
     * @param tokens tokens to add to the corpus
     * @param sourceLabel optional source name reserved for callers
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Ingest a token list into the corpus, treating trailing periods as
     * sentence boundaries and counting both word occurrences and adjacent transitions.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private void ingestTokens(List<String> tokens, String sourceLabel) {
        if (tokens.isEmpty()) {
            return;
        }
        // The first token and each token after a trailing period start a sentence.
        boolean atStart = true;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            boolean end = token.endsWith(".");
            String word = normalize(token.replaceAll("[.]$", ""));
            if (word.isEmpty()) {
                continue;
            }
            // New entries already start at frequency one. Increment existing words only.
            boolean existed = wordsByText.containsKey(word);
            Word w = ensureWord(word, atStart, end);
            if (existed) {
                w.setFrequency(w.getFrequency() + 1);
            }
            totalTokens++;
            // Record the transition from the preceding token when one exists.
            if (i > 0) {
                String prev = normalize(tokens.get(i - 1).replaceAll("[.]$", ""));
                if (!prev.isEmpty()) {
                    recordFollowInternal(prev, word);
                }
            }
            atStart = end;
        }
    }

    /**
     * Increments or creates a transition between normalized corpus words.
     *
     * @param lead normalized leading word
     * @param follow normalized following word
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Update the follower list for a leading word. Increment the existing
     * pair's count or add a pair with count one.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
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

    /**
     * Returns an existing word with merged boundary flags or creates a new entry.
     *
     * @param text normalized word text
     * @param start whether to mark a sentence start
     * @param end whether to mark a sentence end
     * @return the existing or newly created word
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Look up a word by normalized text, preserve existing sentence-
     * boundary flags, and create a new word with a unique ID if needed.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private Word ensureWord(String text, boolean start, boolean end) {
        // Merge boundary flags without changing the frequency of an existing word.
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

    /**
     * Splits text into nonblank tokens and retains periods and apostrophes.
     *
     * @param content text to split
     * @return nonblank tokens in input order
     *
     * ChatGPT Codex was used to create this method
     * Prompt Replace unsupported characters with spaces, split on whitespace, and
     * keep nonblank tokens so ingestion can detect trailing periods.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static List<String> tokenize(String content) {
        // Replace unsupported characters with spaces while keeping periods and apostrophes.
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

    /**
     * Counts distinct normalized words in a token list.
     *
     * @param tokens tokens to count
     * @return the number of distinct normalized words
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Strip trailing periods, normalize each token, and use a set to count
     * distinct nonempty words.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static int countUnique(List<String> tokens) {
        // Compare normalized forms so case and trailing periods do not affect the count.
        Set<String> set = new HashSet<>();
        for (String t : tokens) {
            String n = normalize(t.replaceAll("[.]$", ""));
            if (!n.isEmpty()) {
                set.add(n);
            }
        }
        return set.size();
    }

    /**
     * Trims and lowercases corpus text using a locale-independent conversion.
     *
     * @param text input text, possibly null
     * @return normalized text, or an empty string for null
     *
     * ChatGPT Codex was used to create this method
     * Prompt:  Normalize text for corpus lookups and return an empty string for
     * null input.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Uppercases the first character for sentence display.
     *
     * @param text input text, possibly null or empty
     * @return input with its first character uppercased when present
     *
     * ChatGPT Codex was used to create this method
     * Prompt: Capitalize the first character while passing through null or empty
     * input.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
