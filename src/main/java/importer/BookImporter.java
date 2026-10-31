package importer;

/*****************************************************************************************************************
 * BookImporter.java
 *
 * This class is responsible for reading and processing text files for the Sentence Builder application. It extracts, book title, counts total words and sentences,
 * calculates word frequency, sentence start and end counts, and word transitions.
 *
 * This program also uses JavaFX to create a graphical user interface for selecting and processing text files. Junie AI model was used to help build the JavaFX interface for importing the book. The next line is the prompt
 * "Using java FX, can you make a progress indictor? When I am running this program, I want it to show a progress bar or animated icon that it is in progress"
 *
 * The class also uses JDBC to insert the processed data and statistics into the MySQL database.
 *
 * AI Assistance:
 * Junie AI assisted with the JavaFX interface.
 * ChatGPT assisted with portions of text processing.
 * Written by: Ahmed Sherwani(AJS170005) at the University of Texas at Dallas starting October 17th, 2026.
 ******************************************************************************************************************/

// JavaFX imports by Junie AI
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

public class BookImporter extends Application
{

    //  Hash map that stores unique words and the number of times it occurs in the processed sentence
    final private Map<String, Integer> wordFrequency = new HashMap<>();
    // Hash map that counts how often each word appears at the beginning of the sentence
    final private Map<String, Integer> startCount = new HashMap<>();
    // Hash map that counts how often each word appears at the end of the sentence
    final private Map<String, Integer> endCount = new HashMap<>();
    // Hash map that counts each word pair based on next word and previous and next word
    final private Map<String, Integer> wordTransitions = new HashMap<>();

    // JavaFX UI Components by Junie AI
    private ProgressBar progressBar;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    private Button startButton;
    private TextArea logArea;

    /*****************************************************************************************************
     *  This method initializes the JavaFX UI components and sets up the scene for the application.
     *  This method is created by Junie AI to help build the JavaFX interface for importing the book
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set.
     * Applications may create other stages, if needed, but they will not be
     * primary stages.
     *
     * Written by Ahmed Sherwani on October 26th 2026
     *
     *****************************************************************************************************/
    @Override
    public void start(Stage primaryStage) {
        // Title of the application
        primaryStage.setTitle("Sentence Builder - Book Importer");

        // Title Label in application
        Label titleLabel = new Label("Book Importer");
        // Title Label Style
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Target File Label
        Label fileLabel = new Label("Target File: pg11.txt");
        // Target File Label Style
        fileLabel.setStyle("-fx-text-fill: #555555;");

        // Progress Controls
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        progressBar.setPrefHeight(24);

        // Progress Indicator
        progressIndicator = new ProgressIndicator(0);
        progressIndicator.setPrefSize(45, 45);

        // Progress Box
        HBox progressBox = new HBox(15, progressBar, progressIndicator);
        progressBox.setAlignment(Pos.CENTER);

        // Status Label
        statusLabel = new Label("Ready to import");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Start Button
        startButton = new Button("Start Import");
        startButton.setStyle("-fx-background-color: #2b78e4; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20 8 20; -fx-background-radius: 5;");
        startButton.setOnAction(e -> startImportProcess());

        // Log Area for Import Log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // VBox for layout
        VBox root = new VBox(15, titleLabel, fileLabel, progressBox, statusLabel, startButton, new Label("Import Log:"), logArea);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // Scene for layout
        Scene scene = new Scene(root, 520, 480);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /***********************************************************************************************************************************************************
     * Appends a message to the log TextArea on a new line.
     * Platform.runLater() ensures that the GUI is updated safely from the JavaFX Application Thread when messages are generated by the background task.
     *
     *  This method is written by Junie AI
     *
     * @param message The message to display in the log
     *  Written by Ahmed Sherwani on October 19th 2026
     ***********************************************************************************************************************************************************/
    private void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }


    /*********************************************************************************************************************************************************************
     * Starts the book import process using a background task. The task reads the book file, extracts words and sentences, calculates word statistics, and inserts
     * the processed data into the MySQL database. A background task is used to prevent the JavaFX interface from freezing while the book is processed.
     * The progress indicators and status label are updated to inform the user of the import progress.
     *
     * Some parts of the method are written by Junie AI
     * Written by Ahmed Sherwani on October 25th 2026
     *********************************************************************************************************************************************************************/
    private void startImportProcess() {
        // Disable the start button to prevent user from starting multiple imports at the same time
        startButton.setDisable(true);
        // Clear the log area to display messages for the new import
        logArea.clear();

        // Create a new task to import the book file
        Task<Void> importTask = new Task<>()
        {
            /**
             * Import the book file and update the progress indicator. Check if the book file exists before reading it
             * If the file is missing, stop the import process and report an error
             * Method written by Junie AI
             * @return null
             * @throws Exception if an error occurs while reading the book file
             * Written by Ahmed Sherwani on October 19th 2026
             */
            @Override
            protected Void call() throws Exception {
                File bookFile = new File("pg11.txt");
                if (!bookFile.exists()) {
                    throw new FileNotFoundException("Book file pg11.txt not found.");
                }


                // Count the total number of lines in the book file. The count is used to calculate and display book progress
                updateMessage("Reading total lines in book file...");
                appendLog("Reading book file: " + bookFile.getName());

                long totalLinesCount = Files.lines(bookFile.toPath()).count();


                // Prevent the total line count from being zero to avoid division by zero errors when updating progress bar
                if (totalLinesCount == 0) totalLinesCount = 1;

                // Initialize a StringBuilder to store the book's text
                StringBuilder bookText = new StringBuilder();
                int lineRead = 0;

                // Update the progress indicator to show that the book content is being read
                updateMessage("Reading book content...");

                // Read the book file and append its content to the StringBuilder
                try (Scanner reader = new Scanner(bookFile)) {

                    // Read the book file line. Unnecessary content, such as chapter headings and formatting characters, is skipped so that only the relevant text is processed
                    while (reader.hasNextLine()) {

                        String line = reader.nextLine();

                        // Skip line if it contains * and whitespace
                        if (line.matches("[\\s*]+") && line.contains("*")) {
                            continue;
                        }

                        // Skip chapter headings
                        if (line.trim().matches("(?i)CHAPTER\\s+[IVXLCDM]+\\.")) {
                            continue;
                        }

                        // Skip chapter names in the text
                        if (line.trim().equalsIgnoreCase("Down the Rabbit-Hole") || line.trim().equalsIgnoreCase("The Pool of Tears") || line.trim().equalsIgnoreCase("A Caucus-Race and a Long Tale") ||
                                line.trim().equalsIgnoreCase("The Rabbit Sends in a Little Bill") || line.trim().equalsIgnoreCase("Advice from a Caterpillar") || line.trim().equalsIgnoreCase("Pig and Pepper") || line.trim().equalsIgnoreCase("A Mad Tea-Party") || line.trim().equalsIgnoreCase("The Queen’s Croquet-Ground") ||
                                line.trim().equalsIgnoreCase("The Mock Turtle’s Story") || line.trim().equalsIgnoreCase("The Lobster Quadrille") || line.trim().equalsIgnoreCase("Who Stole the Tarts?") || line.trim().equalsIgnoreCase("Alice’s Evidence")) {
                            continue;
                        }

                        // Read the book file line. Unnecessary content, such as chapter headings and formatting characters, is skipped so that only the relevant text is processed
                        bookText.append(line).append("\n");
                        // Update the progress bar and progress indicator
                        lineRead++;

                        // Update progress bar and progress indicator
                        updateProgress(lineRead, totalLinesCount);

                        // Update the status message and import log every 500 processed lines or when the line count reaches the total
                        if (lineRead % 500 == 0 || lineRead == totalLinesCount) {
                            updateMessage("Read " + lineRead + " / " + totalLinesCount + " lines...");
                            appendLog("Read " + lineRead + " lines...");
                        }
                    }
                }

                // Update the status message and import log when finished reading the book
                appendLog("Finished reading book (" + lineRead + " lines read).");

                // Extract sentences and words
                updateMessage("Extracting sentences and words...");
                // Text from the book
                String text = bookText.toString();
                //  String to hold the first sentence in the book
                String firstSentence = "Alice was beginning to get very tired of sitting";
                // Ending of the book
                String endOfStory = "THE END";

                // Find the start and end indices of the book text
                int startIndex = text.indexOf(firstSentence);
                int endIndex = text.indexOf(endOfStory);

                // Validate the start and end indices
                if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                    throw new IllegalArgumentException("Could not find the specified sentences in the text.");
                }

                // Extract sentences and words
                String[] sentences = extractSentences(text, startIndex, endIndex);
                String[] words = extractWords(text, startIndex, endIndex);

                // Log the number of extracted sentences and words
                appendLog("Extracted " + sentences.length + " sentences and " + words.length + " words.");

                // Update the progress message and log statistics calculation by calling the calculateStatistics method
                updateMessage("Calculating word frequency and transitions...");
                calculateStatistics(sentences);
                appendLog("Statistics calculated: " + wordFrequency.size() + " unique words, " + wordTransitions.size() + " unique transitions.");

                // Message to insert into the database
                updateMessage("Connecting to MySQL database...");
                appendLog("Connecting to database...");

                // Insert book file information and words into the database
                try (Connection connection = database.DatabaseConnection.getConnection())
                {
                    connection.setAutoCommit(false);
                    try {

                        // Insert book file information into imported_files database
                        updateMessage("Inserting book file information...");
                        // Insert book file name, book title, and word count
                        int fileId = insertBookFile(connection, bookFile.getName(), "Alice's Adventures in Wonderland", words.length);
                        // Message for inserted book file record
                        appendLog("Inserted book file record (file_id = " + fileId + ").");

                        // Insert words in the words databas
                        updateMessage("Inserting words...");
                        // Insert word frequency, start count, and end count
                        insertWords(connection, wordFrequency, startCount, endCount);
                        // Message for inserted for total words
                        appendLog("Inserted " + wordFrequency.size() + " words.");

                        // Insert sentences in the sentences databas
                        updateMessage("Inserting sentences...");
                        // Insert sentence and file id in sentences table
                        insertSentences(connection, sentences, fileId);
                        // Message for inserted for total sentences
                        appendLog("Inserted " + sentences.length + " sentences.");

                        // Insert word transitions in the word transitions database
                        updateMessage("Inserting word transitions...");
                        // Insert word transitions into table
                        insertWordTransition(connection, wordTransitions);
                        // Message for inserted word transitions
                        appendLog("Inserted word transitions.");

                        // Commit all changes to the database
                        connection.commit();
                        // Message for successful import
                        updateMessage("Import completed successfully!");
                        appendLog("All data committed to MySQL database successfully!");

                        // Close the database connection
                        connection.close();

                        // Error handling exception
                    } catch (SQLException e) {
                        // Rollback changes if an exception occurs
                        connection.rollback();
                        // Close the database connection
                        connection.close();
                        throw e;
                    }
                }

                // Sets background task progress to 100%
                updateProgress(1.0, 1.0);
                // Return null to indicate successful completion of the task
                return null;
            }
        };

        // Bind progress bar, indicator, and status label to background task
        progressBar.progressProperty().bind(importTask.progressProperty());
        progressIndicator.progressProperty().bind(importTask.progressProperty());
        statusLabel.textProperty().bind(importTask.messageProperty());

        // Re-enable start button and update status label upon successful completion then display success message in green
        importTask.setOnSucceeded(e -> {
            // Disable start button
            startButton.setDisable(false);
            // Set status label text and style
            statusLabel.textProperty().unbind();
            // Print success message in green
            statusLabel.setText("Import completed successfully! \u2714");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 14px; -fx-font-weight: bold;");
        });

        // Re-enable start button and update status label upon failure then display error message in red
        importTask.setOnFailed(e -> {
            // Disable start button
            startButton.setDisable(false);
            // Set status label text and style
            statusLabel.textProperty().unbind();
            // Throw error message
            Throwable ex = importTask.getException();
            // Print error message in red
            statusLabel.setText("Import failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-font-weight: bold;");
            // Append error message to log
            appendLog("Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
            // Print stack trace
            if (ex != null) ex.printStackTrace();
        });

        // Crete a background thread for the import task, mark it as daemon thread so it won't keep the app running if the app closes
        Thread thread = new Thread(importTask);
        thread.setDaemon(true);
        thread.start();
    }

    /*****************************************************************************************
     * NOTE: If running directly in IntelliJ IDEA, run Main.java instead of BookImporter.java
     * to avoid JavaFX runtime component errors, or use 'mvn javafx:run'.
     * This method has been created by Junie AI
     * Written by Ahmed Sherwani on October 27th 2026
     *******************************************************************************************
     */
    public static void main(String[] args)
    {
        launch(args);
    }


    /**
     * Chat GPT was used to help create this method
     * Tokenizes the given text into an array of words.
     * @param text The text to tokenize.
     * @return An array of words.
     *
     * Written by Ahmed Sherwani on October 18th 2026
     */
    public String[] tokenizeWords(String text)
    {
        // Lowercase the text and convert curly quotes to straight apostrophes
        String normalizedText = text.toLowerCase(Locale.ROOT).replace('’', '\'').replace('‘', '\'');

        // Split the text when there's a character that isn't a unicode letter, number, or apostrophe ('). Remove apostrophes at the beginning or end of each word and discard empty strings and collect the remaining words in an array
        return Arrays.stream(normalizedText.split("[^\\p{L}\\p{N}']+")).map(word -> word.replaceAll("^'+|'+$", "")).
                filter(word -> !word.isEmpty()).toArray(String[]::new);
    }

    /*****************************************************************************************************************************************
     * Extracts sentences from the given text between the specified start and end indices.
     * @param text The text to extract sentences from.
     * @param startIndex The start index of the text to extract.
     * @param endIndex The end index of the text to extract.
     * @return An array of sentences.
     *
     * ChatGPT helped with this method for extracting text, removing the underscores, and splitting the text based on punctuation and quotes.
     *
     * Witten by Ahmed Sherwani on October 27th 2026
     ******************************************************************************************************************************************/
    public String[] extractSentences(String text, int startIndex, int endIndex)
    {
        // Extract the relevant portion of the text
        String relevantText = text.substring(startIndex, endIndex).trim();

        // Remove underscores in the sentence
        relevantText = relevantText.replaceAll("(?<!\\w)_(?=\\w)", "").replaceAll("(?<=\\w)_(?!\\w)", "");

        // Split the story into sentences using regex to handle punctuation and quotes
        String[] sentences = relevantText.split("(?<=[.!?][”\"'])\\s+|(?<=[.!?])\\s+");

        // Return the sentences
        return sentences;

    }

    /************************************************************************************
     * Extracts words from the given text between the specified start and end indices.
     * @param text The text to extract words from.
     * @param startIndex The start index of the text to extract.
     * @param endIndex The end index of the text to extract.
     * @return An array of words.
     *
     * Written by Ahmed Sherwani on October 19th 2026
     ************************************************************************************/
    public String[] extractWords(String text, int startIndex, int endIndex)
    {
        // Extract the words from the book
        String relevantText = text.substring(startIndex, endIndex).trim();

        // Remove underscores used for formating
        relevantText = relevantText.replaceAll("(?<!\\w)_(?=\\w)", "").replaceAll("(?<=\\w)_(?!\\w)", "");

        // Extract normalized words
        return tokenizeWords(relevantText);
    }

    /********************************************************************************************************
     * Calculates statistics such as word frequency, start count, and end count for the given sentences.
     * @param sentences An array of sentences to calculate statistics
     *
     *  ChatGPT was to used help with calculateStatistics method. Some ideas and thoughts were provided, but the implementation was completed by Ahmed Sherwani.
     *
     * Written by Ahmed Sherwani on October 20th 2026
     ********************************************************************************************************/
    public void calculateStatistics(String[] sentences){

        // Clear previous statistics
        wordFrequency.clear();
        startCount.clear();
        endCount.clear();
        wordTransitions.clear();

        // Calculate statistics for each sentence
        for (String sentence : sentences)
        {
            String[] words = tokenizeWords(sentence);

            // Skip empty sentences
            if (words.length == 0)
            {
                continue;
            }

            // Calculate word frequency
            for (String word : words)
            {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }

            // Calculate start count
            String firstWord = words[0];
            startCount.put(firstWord, startCount.getOrDefault(firstWord, 0) + 1);

            // Calculate end count
            String lastWord = words[words.length - 1];
            endCount.put(lastWord, endCount.getOrDefault(lastWord, 0) + 1);

            //  For loop to calculate word transitions
            for (int ix = 0; ix < words.length - 1; ix++)
            {
                // String to hold previous and next word
                String previousWord = words[ix];
                String nextWord = words[ix + 1];

                // String to hold transition
                String transition = previousWord + " " + nextWord;

                // Updated with transition's count starting at 1 if it hasn't appeared
                wordTransitions.put(transition,wordTransitions.getOrDefault(transition, 0) + 1);
            }
        }

    }

    /**********************************************************************************************************************
     * Inserts the calculated word statistics into the database.
     * @param connection The connection to the database.
     * @param wordFrequency The frequency of each word in the book.
     * @param startCount The number of times each word appears at the beginning of a sentence.
     * @param endCount The number of times each word appears at the end of a sentence.
     *
     * ChatGPT was to used help with insertWords method. Some ideas and thoughts were provided, but the implementation was completed by Ahmed Sherwani.
     *
     *  Written by Ahmed Sherwani October 22nd 2026
     *********************************************************************************************************************/
    public void insertWords(Connection connection, Map <String, Integer> wordFrequency, Map<String, Integer> startCount, Map<String, Integer> endCount) throws SQLException {
        // Inserting into word table in database
        final String insertStatement = "INSERT INTO words (word_text, frequency, start_count, end_count) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE frequency = frequency + VALUES(frequency), start_count = start_count + VALUES(start_count), end_count = end_count + VALUES(end_count)";

        // Try catch block to handle SQL exceptions during the insertion process
        try (PreparedStatement statement = connection.prepareStatement(insertStatement)) {
            // Loop through each word in the wordFrequency map and set the parameters for the prepared statement
            for (String word : wordFrequency.keySet()) {
                // Set the parameters for the prepared statement
                statement.setString(1, word);
                statement.setInt(2, wordFrequency.get(word));
                statement.setInt(3, startCount.getOrDefault(word, 0));
                statement.setInt(4, endCount.getOrDefault(word, 0));

                // Add the prepared statement to the batch for execution
                statement.addBatch();
            }

            // Execute the batch of insert statements
            statement.executeBatch();
        }
    }

    /****************************************************************************************************************************************************************
     * Inserts word transitions into the database table.
     *
     * @param connection The database connection.
     * @param wordTransitions The map of word transitions.
     * @throws SQLException If an error occurs during the database operation.
     *
     *  ChatGPT was to used help with insertWordTransition method. Some ideas and thoughts were provided, but the implementation was completed by Ahmed Sherwani.
     *  Written by Ahmed Sherwani October 22nd 2026
     *  *************************************************************************************************************************************************************
     */
    public void insertWordTransition(Connection connection, Map<String, Integer> wordTransitions) throws SQLException
    {
        // Create a hash map to store word IDs
        Map<String, Integer> wordsIds = new HashMap<>();

        // Query to retrieve word IDs and text from the words table
        String selectWords = "SELECT word_id, word_text FROM words";

        // Execute the query to retrieve word IDs and text
        try(PreparedStatement statement = connection.prepareStatement(selectWords))
        {
            // Execute the query
            ResultSet resultSet = statement.executeQuery();

            // Populate the wordsIds map with word text and word IDs
            while(resultSet.next())
            {
                wordsIds.put(resultSet.getString("word_text"), resultSet.getInt("word_id"));
            }
        }

        // Query to insert word transitions into the word_transitions table
        String insertWordTransition = "INSERT INTO word_transitions (previous_word_id, next_word_id, transition_count) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE transition_count = transition_count + VALUES(transition_count)";

        // Prepare the statement for inserting word transitions
        try(PreparedStatement statement = connection.prepareStatement(insertWordTransition))
        {
            // For each loop for each word transition pair
            for(Map.Entry<String, Integer> entry : wordTransitions.entrySet())
            {
                // Split the transition into two words into two parts
                String[] pair = entry.getKey().split(" ",2);

                // Check if the transition pair is valid
                if(pair.length != 2)
                {
                    // Throw an exception if the transition pair is invalid
                    throw new SQLException("Invalid word transition pair: " + entry.getKey());
                }

                // Look up the word IDs for the transition pair
                Integer previousWordId = wordsIds.get(pair[0]);
                Integer nextWordId = wordsIds.get(pair[1]);

                // Check if the transition pair is valid and throw an exception if the word ID does not exist
                if(previousWordId == null || nextWordId == null)
                {
                    // Throw an exception if the word ID does not exist
                    throw new SQLException("Missing word ID for transition: "  +entry.getKey());
                }
                // Set the SQL parameters for the transition pair for previous word's ID, next word's ID, and transition count
                statement.setInt(1, previousWordId);
                statement.setInt(2, nextWordId);
                statement.setInt(3, entry.getValue());

                // Add the transition pair to the database
                statement.addBatch();
            }
            // Execute all queued inserts together using batch processing
            statement.executeBatch();
        }
    }

    /***************************************************************************************************************************
     * insertBookFile adds a book file into imported_file database with file name, book title, and total words count.
     *
     * @param connection The database connection.
     * @param fileName The name of the book file.
     * @param bookTitle The title of the book.
     * @param totalWords The total number of words in the book.
     * @return The ID of the inserted book file.
     * @throws SQLException If an error occurs during the database operation.
     *
     *  ChatGPT was used to help with this method. Some ideas and thoughts were provided, but the implementation was completed by Ahmed Sherwani.
     *  Written by Ahmed Sherwani October 24th 2026
     *  *************************************************************************************************************************
     */
    public int insertBookFile(Connection connection, String fileName, String bookTitle, int totalWords) throws SQLException
    {
        // SQL query to insert a book file into imported_files table for file_name, book_title, and total_words columns
        final String insertBookStatement = "INSERT INTO imported_files (file_name, book_title, total_words) VALUES (?, ?, ?)";

        // Prepare statement to insert book file into imported_files table
        try (PreparedStatement statement = connection.prepareStatement(insertBookStatement, Statement.RETURN_GENERATED_KEYS)) {
            // Set values for file_name, book_title, and total_words columns
            statement.setString(1, fileName);
            statement.setString(2, bookTitle);
            statement.setInt(3, totalWords);

            // Execute insert statement
            statement.executeUpdate();

            // Try catch block to retrieve generated file ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                // Check if generatedKeys.next() returns true
                if(generatedKeys.next())
                {
                    // Return generated file ID
                    return generatedKeys.getInt(1);
                }
            }
        }
        // Throw SQLException if generated file ID could not be retrieved
        throw new SQLException("Could not retrieve generated file ID.");
    }


    /**
     * insertSentences method inserts sentences into the database
     *
     * @param connection The database connection.
     * @param sentenceText The array of sentence texts.
     * @param fileID The ID of the file associated with the sentences.
     * @throws SQLException If an error occurs during the database operation.
     *
     * ChatGPT was used to help with this method. Some ideas and thoughts were provided, but the implementation was completed by Ahmed Sherwani.
     *
     *  Written by Ahmed Sherwani October 24th 2026
     */
    public void insertSentences(Connection connection, String[] sentenceText, int fileID) throws SQLException {
        // Initialize the SQL statement to insert sentences into the database by inserting the sentence text, source type, file ID, and batch ID
        final String insertSentenceStatement = "INSERT INTO sentences(sentence_text,source_type, file_id, batch_id) VALUES (?, ?, ?, NULL)";

        // Initialize the PreparedStatement object to execute the SQL statement
        try (PreparedStatement statement = connection.prepareStatement(insertSentenceStatement)) {
            // For each loop to iterate over the sentenceText array
            for (String sentence : sentenceText) {
                // Check if the sentence is blank and skip it if it is
                if (sentence.isBlank()) {
                    continue;
                }
                // Set the sentence text, source type, and file ID for the current sentence
                statement.setString(1, sentence.trim());
                statement.setString(2, "Book");
                statement.setInt(3, fileID);

                // Add the current sentence to the batch
                statement.addBatch();
            }

            // Execute the batch of sentences
            statement.executeBatch();
        }
    }

    }
