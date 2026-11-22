package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * AutocompleteController.java
 *
 * Controls the JavaFX next-word autocomplete screen, retrieves follower
 * suggestions through DataService, and records accepted or new words.
 * It is needed to connect UI events to autocomplete and corpus-update logic.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.model.WordPair;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for next-word autocomplete (suggestions after space / . / ,).
 *
 * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class AutocompleteController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    // Text area used for composing the sentence.
    @FXML private TextArea composeArea;
    // List of follower-word suggestions.
    @FXML private ListView<String> suggestionList;
    // Input field for adding a word that is not in the corpus.
    @FXML private TextField unknownWordField;
    // Displays status and instruction messages.
    @FXML private Label messageLabel;

    // Provides corpus data and autocomplete operations.
    private DataService dataService;
    // Refreshes the application status display after data changes.
    private Runnable statusRefresher;
    // Stores the word for which suggestions are currently shown.
    private String lastLeadingWord = "";

    /**
     * Wires text listeners and keyboard accept for suggestions.
     *
     * @param location
     * The location used to resolve relative paths for the root object, or
     * {@code null} if the location is not known.
     *
     * @param resources
     * The resources used to localize the root object, or {@code null} if
     * the root object was not localized.
     *
     * This method was created with ChatGPT codex
     * Prompt: Initialize the autocomplete JavaFX view by listening for text changes,
     * updating suggestions, supporting Enter-key suggestion acceptance, and allowing
     * a suggestion to be accepted with a double-click.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Refresh suggestions whenever the composed text changes.
        composeArea.textProperty().addListener((obs, oldVal, newVal) -> onTextChanged(newVal));
        // Accept the selected suggestion when Enter is pressed.
        composeArea.setOnKeyPressed(event -> {
            // Only handle Enter when at least one suggestion exists.
            if (event.getCode() == KeyCode.ENTER && !suggestionList.getItems().isEmpty()) {
                event.consume();
                // Select the first suggestion if the user has not chosen one.
                if (suggestionList.getSelectionModel().getSelectedItem() == null) {
                    suggestionList.getSelectionModel().selectFirst();
                }
                onAcceptSuggestion();
            }
        });
        // Accept a suggestion with a double-click.
        suggestionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onAcceptSuggestion();
            }
        });
    }

    /**
     * Provides this screen with the shared data service used for suggestions
     * and corpus updates. The service is injected after the view is created.
     *
     * @param dataService service implementation connected to memory or MySQL
     * This method was created by ChatGPT codex
     * Prompt: Create a setter that injects the shared DataService into the
     * autocomplete controller so it can retrieve suggestions and update corpus data.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setDataService(DataService dataService) {
        // Store the injected service for suggestion and corpus operations.
        this.dataService = dataService;
    }

    /**
     * Receives the host window, although this screen does not open its own
     * dialogs or require direct stage access.
     *
     * @param stage host window provided by the main controller
     * This method was created by ChatGPT codex
     * Prompt: Create a setter that injects the shared DataService into the
     * autocomplete controller so it can retrieve suggestions and update corpus data.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setHostStage(Stage stage) {
        // No stage-specific behavior is needed on this screen.
    }

    /**
     * Stores a callback that updates status values elsewhere in the application.
     * It is called after the word corpus changes.
     *
     * @param refresher status-refresh callback
     *
     * This method was created by ChatGPT codex
     * Prompt: Create a setter that stores a Runnable callback for refreshing
     * application status values after the autocomplete controller changes corpus data.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setStatusRefresher(Runnable refresher) {
        // Save the shared status callback for use after data changes.
        this.statusRefresher = refresher;
    }

    /**
     * Refreshes the suggestions based on the current text in the compose area.
     *
     * This method was created by ChatGPT codex
     * Prompt: Create a refresh method that rechecks the compose-area text and
     * updates the autocomplete suggestions whenever the view is refreshed.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void refresh() {
        // Recheck suggestions using the text still present in the compose area.
        onTextChanged(composeArea.getText());
    }

    /**
     * When the last character is a trigger, load follower suggestions.
     *
     * @param text current compose text
     * This method was created by ChatGPT codex
     * Prompt: Create a text-change handler that checks whether the text ends with
     * a space, period, or comma, extracts the preceding word, retrieves up to
     * twelve follower suggestions, and updates the list and status message.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void onTextChanged(String text) {
        // Wait until the data service is available.
        if (dataService == null) return;
        // Clear suggestions when the compose area is empty.
        if (text == null || text.isEmpty()) {
            lastLeadingWord = "";
            suggestionList.getItems().clear();
            return;
        }
        // Check whether the most recently typed character is a suggestion trigger.
        char last = text.charAt(text.length() - 1);
        // Hide suggestions unless the text ends with a space, period, or comma.
        if (last != ' ' && last != '.' && last != ',') {
            suggestionList.getItems().clear();
            lastLeadingWord = "";
            return;
        }
        // Find the word immediately before the trigger character.
        String leading = extractLeadingWord(text);
        lastLeadingWord = leading;
        // Do not query suggestions when no leading word could be found.
        if (leading.isEmpty()) {
            suggestionList.getItems().clear();
            return;
        }
        // Retrieve the strongest known follower words for the leading word.
        List<WordPair> pairs = dataService.suggestNext(leading, 12);
        // Display each follower with its observed occurrence count.
        suggestionList.getItems().setAll(
                pairs.stream()
                        .map(p -> p.getFollowingWord() + "  (" + p.getCount() + ")")
                        .toList());
        // Select the top suggestion and confirm that suggestions were found.
        if (!pairs.isEmpty()) {
            suggestionList.getSelectionModel().selectFirst();
            messageLabel.setText("Suggestions for \"" + leading + "\"");
        } else {
            // Explain that no known followers exist and offer manual word entry.
            messageLabel.setText("No followers known for \"" + leading + "\". Add a word below.");
        }
    }

    /**
     * Appends the selected suggestion and records the follow count
     *
     * This method was created by ChatGPT codex
     * Prompt: Create an event handler that validates the selected suggestion,
     * extracts the suggested word, records it after the leading word, appends
     * it to the compose area, and refreshes the application status.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onAcceptSuggestion() {
        // Get the suggestion selected in the list.
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        // Ask the user to select a suggestion if none is selected.
        if (selected == null) {
            messageLabel.setText("Select a suggestion first.");
            return;
        }
        // Extract only the word, excluding its displayed frequency count.
        String word = selected.split("\\s+")[0];
        // Record the leading-to-following word transition when a leading word exists.
        if (!lastLeadingWord.isEmpty()) {
            dataService.recordFollow(lastLeadingWord, word);
        }
        // Add the accepted word to the compose area.
        appendWord(word);
        // Display confirmation and refresh application status values.
        messageLabel.setText("Accepted \"" + word + "\"");
        notifyStatus();
    }

    /**
     * Adds an unknown word to the corpus and optionally appends it to the text.
     *
     * This method was created by ChatGPT codex
     * Prompt: Create an event handler that validates text entered in the unknown-word
     * field, adds or increments the word in the corpus, records it after the current
     * leading word if one exists, appends it to the compose area, and refreshes the UI.
     *
     * This method was created by ChatGPT codex
     * Prompt: Create an event handler that validates text entered in the
     * unknown-word field, adds or increments the word in the corpus, records
     * the leading-to-following transition, appends the word to the compose area,
     * and refreshes the suggestions and status.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onAddUnknown() {
        // Trim input or use an empty string when the field has no text.
        String raw = unknownWordField.getText() == null ? "" : unknownWordField.getText().trim();
        // Reject blank input and highlight the field.
        if (raw.isEmpty()) {
            unknownWordField.getStyleClass().add("field-error");
            messageLabel.setText("Enter a word to add.");
            return;
        }
        // Remove the validation error state because the input is now usable.
        unknownWordField.getStyleClass().remove("field-error");
        try {
            // Add the word or increment it if it already exists.
            Word word = dataService.addOrIncrementWord(raw);
            // Record the word as a follower of the current leading word.
            if (!lastLeadingWord.isEmpty()) {
                dataService.recordFollow(lastLeadingWord, word.getText());
            }
            // Append the word to the compose area and clear the input field.
            appendWord(word.getText());
            unknownWordField.clear();
            // Report success, refresh status, and refresh suggestions.
            messageLabel.setText("Added \"" + word.getText() + "\" (freq " + word.getFrequency() + ")");
            notifyStatus();
            onTextChanged(composeArea.getText());
        } catch (IllegalArgumentException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    /**
     * Appends a word to the compose area with appropriate spacing and moves
     * the caret to the end. This keeps accepted and manually added words
     * separate from the suggestion-selection logic.
     *
     * @param word word to append
     *
     * This method was created by ChatGPT codex
     * Prompt: Create a helper method that appends a word to the compose-area
     * text, adds whitespace when needed, moves the caret to the end, and returns
     * focus to the compose area.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void appendWord(String word) {
        // Read the current text or use an empty string.
        String current = composeArea.getText() == null ? "" : composeArea.getText();
        // Add a space before the word unless the text already ends with whitespace.
        if (!current.isEmpty() && !Character.isWhitespace(current.charAt(current.length() - 1))) {
            current = current + " ";
        }
        // Add the word and trailing space, then move focus and the caret.
        composeArea.setText(current + word + " ");
        composeArea.positionCaret(composeArea.getText().length());
        composeArea.requestFocus();
    }

    /**
     * Finds the word immediately before the suggestion trigger at the end of
     * the compose text. It is needed so the controller can query and display
     * words that are likely to follow the current leading word.
     *
     * @param text complete text from the compose area
     * @return normalized leading word, or an empty string if none exists
     *
     * This method was created by ChatGPT codex
     * Prompt: Create a helper that removes trailing whitespace and punctuation
     * from composed text, finds the last word before the removed characters,
     * and returns it in lowercase, or an empty string when no word exists.
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private String extractLeadingWord(String text) {
        String trimmed = text.replaceAll("[\\s.,]+$", "");
        if (trimmed.isEmpty()) {
            return "";
        }
        int idx = trimmed.length() - 1;
        while (idx >= 0 && !Character.isWhitespace(trimmed.charAt(idx))
                && trimmed.charAt(idx) != '.' && trimmed.charAt(idx) != ',') {
            idx--;
        }
        return trimmed.substring(idx + 1).toLowerCase();
    }

    /**
     * Calls the shared status-refresh callback after the corpus changes.
     * This is needed to keep the main interface's word and import totals
     * synchronized with the autocomplete screen.
     *
     * This method was created by ChatGPT codex
     * Prompt: Create a helper that runs the status-refresh callback if one
     * was supplied, keeping main-interface totals synchronized after corpus changes.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void notifyStatus() {
        if (statusRefresher != null) {
            statusRefresher.run();
        }
    }
}
