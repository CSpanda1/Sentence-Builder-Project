package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * WordEditorController.java
 *
 * Controls the JavaFX word library, filters the corpus vocabulary, and
 * displays the selected word. It validates and saves changes to word
 * frequency and sentence-boundary flags through the shared data service.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for viewing and editing word records.
 *
 *  Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class WordEditorController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    // Narrows the vocabulary table by the text entered here.
    @FXML private TextField filterField;
    // Displays the vocabulary records available for selection.
    @FXML private TableView<Word> wordTable;
    // Show word text, frequency, and sentence-boundary flags in the table.
    @FXML private TableColumn<Word, String> colWord;
    @FXML private TableColumn<Word, Number> colFrequency;
    @FXML private TableColumn<Word, Boolean> colStart;
    @FXML private TableColumn<Word, Boolean> colEnd;
    // Displays the selected word alongside its editable frequency and flags.
    @FXML private TextField editTextField;
    @FXML private TextField editFreqField;
    @FXML private CheckBox editStartCheck;
    @FXML private CheckBox editEndCheck;
    // Shows validation feedback and the outcome of a save or refresh.
    @FXML private Label messageLabel;

    // Provides vocabulary queries and metadata updates for the active workspace.
    private DataService dataService;
    // Updates shared totals after a successful save.
    private Runnable statusRefresher;
    // Keeps the selected record and its ID for the next save.
    private Word selected;

    /**
     * Configures the table columns, filter listener, and selection listener.
     *
     * @param location FXML location, if known
     * @param resources FXML resource bundle, if provided
     *
     * Method created by ChatGPT codex
     * Prompt:  Set up the word table to show text, frequency, and sentence-
     * boundary flags. Reload on filter changes and populate the editor when the selected row
     * changes.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Adapt the stored word fields to the JavaFX table columns.
        colWord.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getText()));
        colFrequency.setCellValueFactory(
                c -> new SimpleIntegerProperty(c.getValue().getFrequency()));
        colStart.setCellValueFactory(
                c -> new SimpleBooleanProperty(c.getValue().isSentenceStart()));
        colEnd.setCellValueFactory(
                c -> new SimpleBooleanProperty(c.getValue().isSentenceEnd()));

        // Keep the word text read-only while allowing edits to metadata.
        editTextField.setEditable(false);
        wordTable.setPlaceholder(new Label("No matching words. Try another filter or import a text file."));
        wordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colWord.setSortable(true);
        colFrequency.setSortable(true);

        // Reload the table when the filter changes and sync the editor with row selection.
        filterField.textProperty().addListener((obs, o, n) -> reloadTable());
        wordTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            populateEditor(n);
        });
    }

    /**
     * Stores the service used to read and update vocabulary records.
     *
     * @param dataService active in-memory or MySQL-backed service
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Inject the active DataService so the editor can list filtered
     * words and save metadata changes.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
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
     * Method created and helped by ChatGPT codex
     * Prompt:  Implement the host-stage hook for a word editor that has no
     * window-specific setup.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setHostStage(Stage stage) {
        // Not needed.
    }

    /**
     * Registers the callback used to refresh application totals after a save.
     *
     * @param refresher application status callback
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Store a callback that updates the shared status display after
     * word metadata is saved.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setStatusRefresher(Runnable refresher) {
        this.statusRefresher = refresher;
    }

    /**
     * Reloads the word table when the screen is shown.
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Refresh the displayed vocabulary while retaining the current
     * filter.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void refresh() {
        reloadTable();
    }

    /**
     * Reloads the filtered table and confirms the manual refresh.
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Handle the Refresh button by querying the word list again and
     * displaying a confirmation message.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onRefresh() {
        reloadTable();
        messageLabel.setText("Word list refreshed.");
    }

    /**
     * Validates and saves metadata for the selected word.
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Require a selected word, unchanged text, and a positive integer
     * frequency. Save the frequency and sentence flags, then update the table and shared
     * status if the save succeeds.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onSave() {
        // Require a selected row before validating or saving an edit.
        if (selected == null) {
            messageLabel.setText("Select a word in the table first.");
            return;
        }
        String text = editTextField.getText() == null ? "" : editTextField.getText().trim();
        if (text.isEmpty()) {
            editTextField.getStyleClass().add("field-error");
            messageLabel.setText("Word text cannot be empty.");
            return;
        }
        editTextField.getStyleClass().remove("field-error");

        // Accept only a positive integer frequency and highlight invalid input.
        int freq;
        try {
            freq = Integer.parseInt(editFreqField.getText().trim());
            if (freq < 1) {
                throw new NumberFormatException("Frequency must be >= 1");
            }
            editFreqField.getStyleClass().remove("field-error");
        } catch (NumberFormatException ex) {
            editFreqField.getStyleClass().add("field-error");
            messageLabel.setText("Frequency must be a positive integer.");
            return;
        }

        // Check that the read-only word text still matches the selected record.
        if (!text.equals(selected.getText())) {
            messageLabel.setText("Word text is read-only; edit its frequency or sentence flags.");
            return;
        }
        // Keep the record ID and copy the edited frequency and flags.
        Word updated = new Word(selected.getId(), text, freq,
                editStartCheck.isSelected(), editEndCheck.isSelected());

        // Reload the table and refresh shared totals only after the service saves the update.
        boolean ok = dataService.updateWord(updated);
        if (ok) {
            messageLabel.setText("Saved \"" + text + "\".");
            reloadTable();
            if (statusRefresher != null) {
                statusRefresher.run();
            }
        } else {
            messageLabel.setText("Could not update word (id not found).");
        }
    }

    /**
     * Replaces the table rows with words matching the current filter.
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Skip loading until the data service is available, then query it
     * with the current filter and reapply the table sort.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void reloadTable() {
        // Wait for the shared service before querying the vocabulary.
        if (dataService == null) {
            return;
        }
        String filter = filterField.getText();
        wordTable.setItems(FXCollections.observableArrayList(dataService.listWords(filter)));
        // Reapply the active sort order after replacing the rows.
        wordTable.sort();
    }

    /**
     * Fills the metadata fields from the selected word or clears them when selection is lost.
     *
     * @param word selected word, or null when no row is selected
     *
     * Method created and helped by ChatGPT codex
     * Prompt:  Copy the selected word's text, frequency, and sentence-boundary
     * flags into the editor, or clear the controls when no row is selected.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void populateEditor(Word word) {
        // Clear the editor when the table has no selection.
        if (word == null) {
            editTextField.clear();
            editFreqField.clear();
            editStartCheck.setSelected(false);
            editEndCheck.setSelected(false);
            return;
        }
        editTextField.setText(word.getText());
        editFreqField.setText(String.valueOf(word.getFrequency()));
        editStartCheck.setSelected(word.isSentenceStart());
        editEndCheck.setSelected(word.isSentenceEnd());
    }
}
