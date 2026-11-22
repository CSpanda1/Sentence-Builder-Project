package edu.utdallas.cs4485.team22.sentencebuilder.service;

/*****************************************************************************************************************
 * DataService.java
 *
 * Defines the data operations used by the application, including text import,
 * corpus access, sentence generation, and report retrieval.
 *
 * AI Assistance: ChatGPT Codex
 * Written by: Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 ******************************************************************************************************************/

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
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
 */
public interface DataService {

    /**
     * Imports a plain-text file into the corpus on a background-friendly call.
     *
     * @param path             path to the .txt file
     * @param progressCallback receives progress in [0.0, 1.0]
     * @return import history record
     * @throws Exception if the file cannot be read or processed
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that imports a plain-text file from
     * a given path into the corpus, reports progress through a callback, returns
     * an import-history record on success, and throws an exception if importing
     * fails.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    ImportedFile importText(Path path, DoubleConsumer progressCallback) throws Exception;

    /**
     * Cancels an in-progress import when supported by the implementation.
     *
     * ChatGPT codex was used to create this method
     * Prompt: Add a data-service method that requests cancellation of the
     * currently running text import when the implementation supports it.
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    void cancelImport();

    /**
     * Generates one sentence from a starting word or phrase using the chosen algorithm.
     *
     * @param startWord  starting word or phrase
     * @param algorithm  generation strategy
     * @param maxLength  total word count, including the starting phrase
     * @return the generated sentence record
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that generates and returns one
     * sentence record using the supplied starting word or phrase, generation
     * algorithm, and maximum sentence length.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    GeneratedSentence generate(String startWord, GenerationAlgorithm algorithm, int maxLength);

    /**
     * Generates a batch of sentences with random start words.
     *
     * @param count      number of sentences
     * @param algorithm  generation strategy
     * @param maxLength  total word count per sentence
     * @return generated sentences
     *
     * ChatGPT codes was used to create this method
     * Prompt: Define a data-service method that generates the requested number
     * of sentences using randomly selected starting words, the chosen generation
     * algorithm, and the specified maximum length, then returns them as a list.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     *
     */
    List<GeneratedSentence> generateBatch(int count, GenerationAlgorithm algorithm, int maxLength);

    /**
     * Returns ranked next-word suggestions for the given leading word.
     *
     * @param leadingWord word immediately before the caret
     * @param limit       maximum suggestions
     * @return follower pairs sorted by descending count
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that returns up to the requested
     * number of likely following words for a given leading word, ordered from
     * most frequently observed pair to least frequent.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    List<WordPair> suggestNext(String leadingWord, int limit);

    /**
     * Adds an unknown word to the corpus (or increments frequency if present).
     *
     * @param wordText normalized word text
     * @return the stored word
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that adds a word to the corpus if
     * it is not already present, or increments its frequency if it exists,
     * then returns the stored Word record.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    Word addOrIncrementWord(String wordText);

    /**
     * Records that {@code following} was chosen after {@code leading}.
     *
     * @param leadingWord   previous word
     * @param followingWord chosen suggestion or typed word
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that records a transition from a
     * leading word to the word selected or typed immediately after it, updating
     * the corpus's word-pair data.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    void recordFollow(String leadingWord, String followingWord);

    /**
     * Lists all words, optionally filtered by a case-insensitive substring.
     *
     * @param filter optional filter text; null or blank means no filter
     * @return matching words
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that returns corpus words, applying
     * an optional case-insensitive substring filter; treat a null or blank
     * filter as a request to return all words.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    List<Word> listWords(String filter);

    /**
     * Updates editable fields on an existing word.
     *
     * @param word word with updated values (id must exist)
     * @return true if updated
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that updates an existing corpus
     * word using the supplied Word object, identified by its existing ID,
     * and returns whether the update succeeded.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    boolean updateWord(Word word);

    /**
     * Finds a word by exact (normalized) text.
     *
     * @param text word text
     * @return optional word
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that finds a corpus word by its
     * exact normalized text and returns an Optional containing the word when
     * found, or an empty Optional when it is not present.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    Optional<Word> findWord(String text);

    /**
     * @return all imported-file history records
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that retrieves and returns all
     * records describing text files previously imported into the corpus.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    List<ImportedFile> listImportedFiles();

    /**
     * @return all generated sentences (newest first)
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that returns all generated sentence
     * records, ordered from newest to oldest.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    List<GeneratedSentence> listSentences();

    /**
     * @return total token count across the corpus
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that returns the total number of
     * word tokens across all text currently stored in the corpus.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    int getTotalWordCount();

    /**
     * @return number of distinct words
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that returns the number of distinct
     * words currently stored in the corpus.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    int getUniqueWordCount();

    /**
     * @return display name of the most recent import, or empty
     *
     * ChatGPT codex was used to create this method
     * Prompt: Define a data-service method that returns the display name of
     * the most recently imported file, or an empty Optional if no file has
     * been imported.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 17th, 2026.
     */
    Optional<String> getLastImportName();
}
