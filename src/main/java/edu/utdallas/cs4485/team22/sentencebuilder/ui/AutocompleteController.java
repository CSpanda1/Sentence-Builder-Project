package edu.utdallas.cs4485.team22.sentencebuilder.ui;

import edu.utdallas.cs4485.team22.sentencebuilder.autocomplete.AutoCompleteRecorder;
import edu.utdallas.cs4485.team22.sentencebuilder.autocomplete.AutoCompleteSuggester;
import edu.utdallas.cs4485.team22.sentencebuilder.generation.FollowerProvider;
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
 * Controller for next-word autocomplete (Umair suggester + DataService fallback).
 *
 * @author Ved Mahant
 * @author Umair Malik
 * @date 2026-10-16
 */
public class AutocompleteController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    @FXML private TextArea composeArea;
    @FXML private ListView<String> suggestionList;
    @FXML private TextField unknownWordField;
    @FXML private Label messageLabel;

    private DataService dataService;
    private AutoCompleteSuggester suggester;
    private Runnable statusRefresher;
    private String lastLeadingWord = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        composeArea.textProperty().addListener((obs, oldVal, newVal) -> onTextChanged(newVal));
        composeArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !suggestionList.getItems().isEmpty()) {
                event.consume();
                if (suggestionList.getSelectionModel().getSelectedItem() == null) {
                    suggestionList.getSelectionModel().selectFirst();
                }
                onAcceptSuggestion();
            }
        });
        suggestionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onAcceptSuggestion();
            }
        });
    }

    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
        if (dataService instanceof FollowerProvider provider
                && dataService instanceof AutoCompleteRecorder recorder) {
            this.suggester = new AutoCompleteSuggester(provider, recorder);
        } else {
            this.suggester = null;
        }
    }

    @Override
    public void setHostStage(Stage stage) {
        // Not needed.
    }

    @Override
    public void setStatusRefresher(Runnable refresher) {
        this.statusRefresher = refresher;
    }

    @Override
    public void refresh() {
        onTextChanged(composeArea.getText());
    }

    private void onTextChanged(String text) {
        if (text == null || text.isEmpty()) {
            suggestionList.getItems().clear();
            return;
        }
        if (suggester != null) {
            List<String> suggestions = suggester.suggest(text);
            suggestionList.getItems().setAll(suggestions);
            lastLeadingWord = suggester.lastCompleteWord(text);
            if (!suggestions.isEmpty()) {
                suggestionList.getSelectionModel().selectFirst();
                messageLabel.setText("Suggestions for \"" + lastLeadingWord + "\"");
            } else if (endsWithTrigger(text)) {
                messageLabel.setText("No followers known for \"" + lastLeadingWord + "\".");
            }
            return;
        }

        char last = text.charAt(text.length() - 1);
        if (last != ' ' && last != '.' && last != ',') {
            return;
        }
        String leading = extractLeadingWord(text);
        lastLeadingWord = leading;
        if (leading.isEmpty()) {
            suggestionList.getItems().clear();
            return;
        }
        List<WordPair> pairs = dataService.suggestNext(leading, 12);
        suggestionList.getItems().setAll(
                pairs.stream()
                        .map(p -> p.getFollowingWord() + "  (" + p.getCount() + ")")
                        .toList());
        if (!pairs.isEmpty()) {
            suggestionList.getSelectionModel().selectFirst();
            messageLabel.setText("Suggestions for \"" + leading + "\"");
        } else {
            messageLabel.setText("No followers known for \"" + leading + "\".");
        }
    }

    @FXML
    public void onAcceptSuggestion() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Select a suggestion first.");
            return;
        }
        String word = selected.split("\\s+")[0];
        if (suggester != null) {
            word = suggester.acceptSuggestion(composeArea.getText(), word);
        } else if (!lastLeadingWord.isEmpty()) {
            dataService.recordFollow(lastLeadingWord, word);
        }
        appendWord(word);
        messageLabel.setText("Accepted \"" + word + "\"");
        notifyStatus();
    }

    @FXML
    public void onAddUnknown() {
        String raw = unknownWordField.getText() == null ? "" : unknownWordField.getText().trim();
        if (raw.isEmpty()) {
            unknownWordField.getStyleClass().add("field-error");
            messageLabel.setText("Enter a word to add.");
            return;
        }
        unknownWordField.getStyleClass().remove("field-error");
        try {
            String word;
            if (suggester != null) {
                word = suggester.recordTypedWord(composeArea.getText(), raw);
            } else {
                Word created = dataService.addOrIncrementWord(raw);
                word = created.getText();
                if (!lastLeadingWord.isEmpty()) {
                    dataService.recordFollow(lastLeadingWord, word);
                }
            }
            appendWord(word);
            unknownWordField.clear();
            messageLabel.setText("Added \"" + word + "\"");
            notifyStatus();
            onTextChanged(composeArea.getText());
        } catch (IllegalArgumentException ex) {
            messageLabel.setText(ex.getMessage());
        }
    }

    private void appendWord(String word) {
        String current = composeArea.getText() == null ? "" : composeArea.getText();
        if (!current.isEmpty() && !Character.isWhitespace(current.charAt(current.length() - 1))) {
            current = current + " ";
        }
        composeArea.setText(current + word + " ");
        composeArea.positionCaret(composeArea.getText().length());
        composeArea.requestFocus();
    }

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

    private boolean endsWithTrigger(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char c = text.charAt(text.length() - 1);
        return c == ' ' || c == '.' || c == ',';
    }

    private void notifyStatus() {
        if (statusRefresher != null) {
            statusRefresher.run();
        }
    }
}
