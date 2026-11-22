package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * GenerationController.java
 *
 * Controls the JavaFX sentence generator, validates starting phrases and
 * generation settings, and displays individual and batch results.
 * It also handles result copying and refreshes the shared corpus totals.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.model.GeneratedSentence;
import edu.utdallas.cs4485.team22.sentencebuilder.model.GenerationAlgorithm;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the sentence generation screen.
 *
 * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class GenerationController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    // Accepts a starting word or phrase for single-sentence generation.
    @FXML private TextField startWordField;
    // Selects the strategy for choosing each follower.
    @FXML private ComboBox<GenerationAlgorithm> algorithmCombo;
    // Selects the full word count, including the starting phrase.
    @FXML private Spinner<Integer> lengthSpinner;
    // Shows validation and generation errors.
    @FXML private Label validationLabel;
    // Explains how the selected algorithm chooses followers.
    @FXML private Label algorithmHint;
    // Shows the session result count or clipboard confirmation.
    @FXML private Label resultCount;
    // Copies the results when the list contains at least one entry.
    @FXML private javafx.scene.control.Button copyButton;
    // Clears the current session's displayed results.
    @FXML private javafx.scene.control.Button clearButton;
    // Displays formatted generation results for this session.
    @FXML private ListView<String> resultsList;

    // Provides generation and vocabulary operations.
    private DataService dataService;
    // Updates the application's shared status after generation.
    private Runnable statusRefresher;

    /**
     * Initializes algorithm choices and the length spinner.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Initialize the JavaFX generator with algorithm choices, a word-
     * count control, Enter-key submission, and bindings for validation and results.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Default to weighted selection and configure the supported sentence lengths.
        algorithmCombo.setItems(FXCollections.observableArrayList(GenerationAlgorithm.values()));
        algorithmCombo.getSelectionModel().select(GenerationAlgorithm.PROBABILITY_WEIGHTED);
        lengthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 40, 12));
        lengthSpinner.setEditable(false);
        // Keep the hint in sync and let Enter submit the starting phrase.
        algorithmCombo.valueProperty().addListener((obs, before, after) -> describeAlgorithm());
        describeAlgorithm();
        startWordField.setOnAction(e -> onGenerate());
        // Enable result actions and update the session count as results change.
        copyButton.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(resultsList.getItems()));
        clearButton.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(resultsList.getItems()));
        resultsList.getItems().addListener((javafx.collections.ListChangeListener<String>) change ->
            resultCount.setText(resultsList.getItems().size() + " in this session"));
        // Collapse the validation label when there is no message.
        validationLabel.managedProperty().bind(validationLabel.visibleProperty());
        validationLabel.visibleProperty().bind(validationLabel.textProperty().isNotEmpty());
        resultsList.setPlaceholder(new Label("Your generated sentences will appear here."));
        // Constrain each result cell to the list width so longer sentences can wrap.
        resultsList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            { prefWidthProperty().bind(list.widthProperty().subtract(24)); setMaxWidth(Double.MAX_VALUE); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                // Clear the text of empty cells when the list reuses them.
                setText(empty ? null : item); setWrapText(true);
            }
        });
    }

    /**
     * Stores the service used to generate sentences and look up starting words.
     *
     * @param dataService active in-memory or MySQL-backed service
     *
     * This method was created by ChatGPT codex
     * Prompt:  Store the active DataService so the generator can request
     * sentences and look up vocabulary.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Receives the host window through the shared controller interface.
     *
     * @param stage host window supplied by the main controller
     *
     * This method was created by ChatGPT codex
     * Prompt:  Implement the shared host-stage hook for a generator screen
     * that has no window-specific setup.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setHostStage(Stage stage) {
        // Not needed for this screen.
    }

    /**
     * Registers the callback used to refresh shared status values after generation.
     *
     * @param refresher application status callback
     *
     * This method was created by ChatGPT codex
     * Prompt:  Store a callback that refreshes application totals after
     * generation.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setStatusRefresher(Runnable refresher) {
        this.statusRefresher = refresher;
    }

    /**
     * Keeps the session's generated results when the user returns to this screen.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Implement the view refresh hook while keeping the generated
     * results for the current session.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void refresh() {
        // Results list is session-local; status bar handles corpus counts.
    }

    /**
     * Validates input and generates one sentence.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Validate the starting phrase, generate one sentence with the
     * selected algorithm and word count, then display the result or any service error.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onGenerate() {
        clearValidation();
        // Trim the starting phrase before checking for missing input.
        String start = startWordField.getText() == null ? "" : startWordField.getText().trim();
        // Highlight the input field and stop when the starting phrase is blank.
        if (start.isEmpty()) {
            showValidation("Enter a start word or phrase.");
            startWordField.getStyleClass().add("field-error");
            return;
        }
        startWordField.getStyleClass().remove("field-error");
        try {
            // Pass the selected algorithm and word count to the active service.
            GeneratedSentence sentence = dataService.generate(
                    start,
                    algorithmCombo.getValue(),
                    lengthSpinner.getValue());
            // Add the result and refresh counts after the service stores it.
            prependResult(format(sentence));
            notifyStatus();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showValidation(ex.getMessage());
        }
    }

    /**
     * Generates a batch of five random-start sentences.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Generate five sentences from random starting words and add them
     * to the result list in batch order.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onBatch() {
        clearValidation();
        startWordField.getStyleClass().remove("field-error");
        try {
        // Use the same algorithm and word count for all five random starts.
        List<GeneratedSentence> batch = dataService.generateBatch(
                5,
                algorithmCombo.getValue(),
                lengthSpinner.getValue());
        // Walk the batch backward because each result is inserted at the top.
        for (int i = batch.size() - 1; i >= 0; i--) {
            prependResult(format(batch.get(i)));
        }
        notifyStatus();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showValidation(ex.getMessage());
        }
    }

    /**
     * Clears the results list.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Clear the displayed session results and dismiss any validation
     * message.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onClear() {
        resultsList.getItems().clear();
        clearValidation();
    }

    /**
     * Updates the hint to explain how the selected algorithm chooses a follower.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Show a hint explaining how the selected algorithm chooses the
     * next word.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void describeAlgorithm() {
        if (algorithmCombo.getValue() == null) return;
        algorithmHint.setText(switch (algorithmCombo.getValue()) {
            case PROBABILITY_WEIGHTED -> "Probability-weighted · Common followers are more likely; rare followers still get a chance.";
            case MOST_FREQUENT -> "Most frequent · Always chooses the most common known follower.";
            case RANDOM_WALK -> "Random walk · Gives every known follower an equal chance.";
        });
    }

    /**
     * Suggests the most frequent known sentence-start word, if available.
     * Uses the first vocabulary entry when no word is marked as a sentence start.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Suggest the most frequent sentence-start word, fall back to the
     * first vocabulary entry, and show a message if the corpus is empty.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML public void onTryStart() {
        // Prefer the most frequent sentence-start word and fall back to any available word.
        var vocabulary = dataService.listWords(null);
        var word = vocabulary.stream().filter(edu.utdallas.cs4485.team22.sentencebuilder.model.Word::isSentenceStart)
                .max(java.util.Comparator.comparingInt(edu.utdallas.cs4485.team22.sentencebuilder.model.Word::getFrequency))
                .or(() -> vocabulary.stream().findFirst());
        if (word.isEmpty()) { showValidation("Import text first to give the generator a vocabulary."); return; }
        startWordField.setText(word.get().getText());
        startWordField.getStyleClass().remove("field-error"); clearValidation(); startWordField.requestFocus();
    }

    /**
     * Copies the displayed results to the system clipboard, one per line.
     *
     * Prompt:  Copy the displayed sentences to the system clipboard in list
     * order and confirm how many were copied.
     *
     * This method was created by ChatGPT codex
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML public void onCopy() {
        if (resultsList.getItems().isEmpty()) return;
        // Copy the displayed lines in their current order.
        var content = new javafx.scene.input.ClipboardContent();
        content.putString(String.join(System.lineSeparator(), resultsList.getItems()));
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        resultCount.setText("Copied " + resultsList.getItems().size() + " results to clipboard");
    }

    /**
     * Formats a result for the list and identifies duplicates.
     *
     * @param sentence generated sentence record
     * @return formatted result line
     *
     * Prompt:  Format a generated sentence with its algorithm name and mark it
     * if it was flagged as a duplicate.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private String format(GeneratedSentence sentence) {
        // Mark duplicate sentences in the displayed and copied results.
        String flag = sentence.isDuplicate() ? " [duplicate]" : "";
        return "[" + sentence.getAlgorithm() + "]" + flag + "  " + sentence.getText();
    }

    /**
     * Inserts a formatted result at the top of the session list.
     *
     * @param line formatted result to display
     *
     * This method was created by ChatGPT codex
     * Prompt:  Insert a formatted sentence at the top of the displayed
     * results.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void prependResult(String line) {
        resultsList.getItems().add(0, line);
    }

    /**
     * Displays an input or generation error in the validation label.
     *
     * @param message message to show
     *
     * This method was created by ChatGPT codex
     * Prompt:  Set the validation label to the supplied input or generation
     * error.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void showValidation(String message) {
        validationLabel.setText(message);
    }

    /**
     * Clears the validation text so the bound label is hidden.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Clear the validation text so its bound label becomes hidden.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void clearValidation() {
        validationLabel.setText("");
    }

    /**
     * Runs the shared status callback when one is registered.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Run the shared status refresh callback if one was registered.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void notifyStatus() {
        if (statusRefresher != null) {
            statusRefresher.run();
        }
    }
}
