package edu.utdallas.cs4485.team22.sentencebuilder.ui;

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
 * @author Ved Mahant
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class WordEditorController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    @FXML private TextField filterField;
    @FXML private TableView<Word> wordTable;
    @FXML private TableColumn<Word, String> colWord;
    @FXML private TableColumn<Word, Number> colFrequency;
    @FXML private TableColumn<Word, Boolean> colStart;
    @FXML private TableColumn<Word, Boolean> colEnd;
    @FXML private TextField editTextField;
    @FXML private TextField editFreqField;
    @FXML private CheckBox editStartCheck;
    @FXML private CheckBox editEndCheck;
    @FXML private Label messageLabel;

    private DataService dataService;
    private Runnable statusRefresher;
    private Word selected;

    /**
     * Configures table columns, filter listener, and selection binding.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colWord.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getText()));
        colFrequency.setCellValueFactory(
                c -> new SimpleIntegerProperty(c.getValue().getFrequency()));
        colStart.setCellValueFactory(
                c -> new SimpleBooleanProperty(c.getValue().isSentenceStart()));
        colEnd.setCellValueFactory(
                c -> new SimpleBooleanProperty(c.getValue().isSentenceEnd()));

        colWord.setSortable(true);
        colFrequency.setSortable(true);

        filterField.textProperty().addListener((obs, o, n) -> reloadTable());
        wordTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            populateEditor(n);
        });
    }

    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
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
        reloadTable();
    }

    /**
     * Reloads the table from the data service using the current filter.
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @FXML
    public void onRefresh() {
        reloadTable();
        messageLabel.setText("Word list refreshed.");
    }

    /**
     * Saves edits for the selected word after validating frequency.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onSave() {
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

        selected.setText(text);
        selected.setFrequency(freq);
        selected.setSentenceStart(editStartCheck.isSelected());
        selected.setSentenceEnd(editEndCheck.isSelected());

        boolean ok = dataService.updateWord(selected);
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

    private void reloadTable() {
        if (dataService == null) {
            return;
        }
        String filter = filterField.getText();
        wordTable.setItems(FXCollections.observableArrayList(dataService.listWords(filter)));
        wordTable.sort();
    }

    private void populateEditor(Word word) {
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
