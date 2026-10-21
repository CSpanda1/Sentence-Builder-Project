package edu.utdallas.cs4485.team22.sentencebuilder.ui;

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
 * @author Ved Mahant
 * @date 2026-10-16
 */
public class GenerationController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    @FXML private TextField startWordField;
    @FXML private ComboBox<GenerationAlgorithm> algorithmCombo;
    @FXML private Spinner<Integer> lengthSpinner;
    @FXML private Label validationLabel;
    @FXML private ListView<String> resultsList;

    private DataService dataService;
    private Runnable statusRefresher;

    /**
     * Initializes algorithm choices and the length spinner.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        algorithmCombo.setItems(FXCollections.observableArrayList(GenerationAlgorithm.values()));
        algorithmCombo.getSelectionModel().select(GenerationAlgorithm.PROBABILITY_WEIGHTED);
        lengthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 40, 12));
        lengthSpinner.setEditable(true);
    }

    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void setHostStage(Stage stage) {
        // Not needed for this screen.
    }

    @Override
    public void setStatusRefresher(Runnable refresher) {
        this.statusRefresher = refresher;
    }

    @Override
    public void refresh() {
        // Results list is session-local; status bar handles corpus counts.
    }

    /**
     * Validates input and generates one sentence.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onGenerate() {
        clearValidation();
        String start = startWordField.getText() == null ? "" : startWordField.getText().trim();
        if (start.isEmpty()) {
            showValidation("Start word is required.");
            startWordField.getStyleClass().add("field-error");
            return;
        }
        startWordField.getStyleClass().remove("field-error");
        try {
            GeneratedSentence sentence = dataService.generate(
                    start,
                    algorithmCombo.getValue(),
                    lengthSpinner.getValue());
            prependResult(format(sentence));
            notifyStatus();
        } catch (IllegalArgumentException ex) {
            showValidation(ex.getMessage());
        }
    }

    /**
     * Generates a batch of five random-start sentences.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onBatch() {
        clearValidation();
        startWordField.getStyleClass().remove("field-error");
        List<GeneratedSentence> batch = dataService.generateBatch(
                5,
                algorithmCombo.getValue(),
                lengthSpinner.getValue());
        for (int i = batch.size() - 1; i >= 0; i--) {
            prependResult(format(batch.get(i)));
        }
        notifyStatus();
    }

    /**
     * Clears the results list.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onClear() {
        resultsList.getItems().clear();
        clearValidation();
    }

    private String format(GeneratedSentence sentence) {
        String flag = sentence.isDuplicate() ? " [duplicate]" : "";
        return "[" + sentence.getAlgorithm() + "]" + flag + "  " + sentence.getText();
    }

    private void prependResult(String line) {
        resultsList.getItems().add(0, line);
    }

    private void showValidation(String message) {
        validationLabel.setText(message);
    }

    private void clearValidation() {
        validationLabel.setText("");
    }

    private void notifyStatus() {
        if (statusRefresher != null) {
            statusRefresher.run();
        }
    }
}
