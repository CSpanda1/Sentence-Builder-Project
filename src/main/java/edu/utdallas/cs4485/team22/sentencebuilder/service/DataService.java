package edu.utdallas.cs4485.team22.sentencebuilder.service;

import edu.utdallas.cs4485.team22.sentencebuilder.model.GeneratedSentence;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.model.ImportedFile;
import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleConsumer;

/**
 * Data-access contract used by the JavaFX UI.
 *
 * <p>WP7 builds against this interface with {@link StubDataService}.
 * WP4 will supply a JDBC implementation without changing controllers.</p>
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public interface DataService {

    /**
     * Imports a plain-text file into the corpus on a background-friendly call.
     *
     * @param path             path to the .txt file
     * @param progressCallback receives progress in [0.0, 1.0]
     * @return import history record
     * @throws Exception if the file cannot be read or processed
     */
    ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception;

    /**
     * Cancels an in-progress import when supported by the implementation.
     */
    void cancelImport();

    /**
     * Generates one sentence from a start word using the chosen algorithm.
     *
     * @param startWord  seed word
     * @param algorithm  generation strategy
     * @param maxLength  maximum number of words
     * @return the generated sentence record
     */
    GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength);

    /**
     * Generates a batch of sentences with random start words.
     *
     * @param count      number of sentences
     * @param algorithm  generation strategy
     * @param maxLength  maximum words per sentence
     * @return generated sentences
     */
    List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength);

    /**
     * Returns ranked next-word suggestions for the given leading word.
     *
     * @param leadingWord word immediately before the caret
     * @param limit       maximum suggestions
     * @return follower pairs sorted by descending count
     */
    List<WordPair> suggestNext(String leadingWord, int limit);

    /**
     * Adds an unknown word to the corpus (or increments frequency if present).
     *
     * @param wordText normalized word text
     * @return the stored word
     */
    Word addOrIncrementWord(String wordText);

    /**
     * Records that {@code following} was chosen after {@code leading}.
     *
     * @param leadingWord   previous word
     * @param followingWord chosen suggestion or typed word
     */
    void recordFollow(String leadingWord, String followingWord);

    /**
     * Lists all words, optionally filtered by a case-insensitive substring.
     *
     * @param filter optional filter text; null or blank means no filter
     * @return matching words
     */
    List<Word> listWords(String filter);

    /**
     * Updates editable fields on an existing word.
     *
     * @param word word with updated values (id must exist)
     * @return true if updated
     */
    boolean updateWord(Word word);

    /**
     * Finds a word by exact (normalized) text.
     *
     * @param text word text
     * @return optional word
     */
    Optional<Word> findWord(String text);

    /**
     * @return all imported-file history records
     */
    List<ImportedFile> listImportedFiles();

    /**
     * @return all generated sentences (newest first)
     */
    List<GeneratedSentence> listSentences();

    /**
     * @return total token count across the corpus
     */
    int getTotalWordCount();

    /**
     * @return number of distinct words
     */
    int getUniqueWordCount();

    /**
     * @return display name of the most recent import, or empty
     */
    Optional<String> getLastImportName();
}
