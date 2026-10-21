package edu.utdallas.cs4485.team22.sentencebuilder.ui;

import edu.utdallas.cs4485.team22.sentencebuilder.model.GeneratedSentence;
import edu.utdallas.cs4485.team22.sentencebuilder.model.ImportedFile;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for import history, sentence history, and corpus statistics.
 *
 * @author Ahmed Sherwani
 * @author Ved Mahant
 * @date 2026-10-16
 */
public class ReportsController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableView<ImportedFile> filesTable;
    @FXML private TableColumn<ImportedFile, String> colFileName;
    @FXML private TableColumn<ImportedFile, String> colFilePath;
    @FXML private TableColumn<ImportedFile, Number> colFileWords;
    @FXML private TableColumn<ImportedFile, Number> colFileUnique;
    @FXML private TableColumn<ImportedFile, String> colFileWhen;

    @FXML private TableView<GeneratedSentence> sentencesTable;
    @FXML private TableColumn<GeneratedSentence, String> colSentText;
    @FXML private TableColumn<GeneratedSentence, String> colSentAlgo;
    @FXML private TableColumn<GeneratedSentence, String> colSentStart;
    @FXML private TableColumn<GeneratedSentence, Boolean> colSentDup;
    @FXML private TableColumn<GeneratedSentence, String> colSentWhen;

    @FXML private Label statsTotal;
    @FXML private Label statsUnique;
    @FXML private Label statsFiles;
    @FXML private Label statsSentences;
    @FXML private Label statsDuplicates;
    @FXML private Label statsLastImport;

    private DataService dataService;

    /**
     * Binds table column value factories.
     *
     * @author Ahmed Sherwani
     * @date 2026-10-16
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colFileName.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getFileName()));
        colFilePath.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getFilePath()));
        colFileWords.setCellValueFactory(
                c -> new SimpleIntegerProperty(c.getValue().getWordCount()));
        colFileUnique.setCellValueFactory(
                c -> new SimpleIntegerProperty(c.getValue().getUniqueWordCount()));
        colFileWhen.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getImportedAt().format(TIME_FMT)));

        colSentText.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getText()));
        colSentAlgo.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getAlgorithm()));
        colSentStart.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getStartWord()));
        colSentDup.setCellValueFactory(
                c -> new SimpleBooleanProperty(c.getValue().isDuplicate()));
        colSentWhen.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getCreatedAt().format(TIME_FMT)));
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
        // Reports are read-only.
    }

    /**
     * Reloads all report tables and statistics from the data service.
     *
     * @author Ahmed Sherwani
     * @date 2026-10-16
     */
    @Override
    public void refresh() {
        if (dataService == null) {
            return;
        }
        List<ImportedFile> files = dataService.listImportedFiles();
        List<GeneratedSentence> sentences = dataService.listSentences();

        filesTable.setItems(FXCollections.observableArrayList(files));
        sentencesTable.setItems(FXCollections.observableArrayList(sentences));

        long dupes = sentences.stream().filter(GeneratedSentence::isDuplicate).count();
        statsTotal.setText("Total tokens: " + dataService.getTotalWordCount());
        statsUnique.setText("Unique words: " + dataService.getUniqueWordCount());
        statsFiles.setText("Imported files: " + files.size());
        statsSentences.setText("Generated sentences: " + sentences.size());
        statsDuplicates.setText("Duplicate sentences: " + dupes);
        statsLastImport.setText("Last import: "
                + dataService.getLastImportName().orElse("—"));
    }
}
