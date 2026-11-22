package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * ReportsController.java
 *
 * Controls the JavaFX reports screen for corpus statistics, import
 * history, and generated sentences. It reads reports from the active
 * data service and links empty reports to the import screen.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

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
 * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) at the University of Texas at Dallas starting October 16th, 2026.
 */
public class ReportsController
        implements Initializable, MainController.ServiceAware, MainController.Refreshable {

    // Formats import and sentence timestamps for display in report tables.
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Displays imported-file history and its columns.
    @FXML private TableView<ImportedFile> filesTable;
    @FXML private TableColumn<ImportedFile, String> colFileName;
    @FXML private TableColumn<ImportedFile, String> colFilePath;
    @FXML private TableColumn<ImportedFile, Number> colFileWords;
    @FXML private TableColumn<ImportedFile, Number> colFileUnique;
    @FXML private TableColumn<ImportedFile, String> colFileWhen;

    // Displays generated-sentence history and its columns.
    @FXML private TableView<GeneratedSentence> sentencesTable;
    @FXML private TableColumn<GeneratedSentence, String> colSentText;
    @FXML private TableColumn<GeneratedSentence, String> colSentAlgo;
    @FXML private TableColumn<GeneratedSentence, String> colSentStart;
    @FXML private TableColumn<GeneratedSentence, Boolean> colSentDup;
    @FXML private TableColumn<GeneratedSentence, String> colSentWhen;

    // Displays summary corpus and report statistics.
    @FXML private Label statsTotal;
    @FXML private Label statsUnique;
    @FXML private Label statsFiles;
    @FXML private Label statsSentences;
    @FXML private Label statsDuplicates;
    @FXML private Label statsLastImport;

    // Opens the import screen when the empty-state action is clicked.
    private Runnable importNavigator;

    /**
     * Registers the action that opens the import screen from an empty report.
     *
     * @param action callback that opens Import
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Save an import navigation callback so a table placeholder can
     * send users to the import screen.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void setImportNavigator(Runnable action) { importNavigator = action; }

    /**
     * Builds a placeholder with guidance and an import button for an empty table.
     *
     * @param icon glyph displayed above the heading
     * @param title placeholder heading
     * @param copy explanatory text
     * @return placeholder node for a report table
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Create a centered JavaFX placeholder with an icon, heading,
     * wrapped guidance, and a button that runs the import callback when available.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private javafx.scene.Node emptyState(String icon, String title, String copy) {
        var box = new javafx.scene.layout.VBox(12);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setMaxWidth(390);
        var glyph = new Label(icon); glyph.getStyleClass().add("empty-icon");
        var heading = new Label(title); heading.getStyleClass().add("empty-title");
        var hint = new Label(copy); hint.setWrapText(true); hint.setMaxWidth(320);
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); hint.getStyleClass().add("guide-copy");
        // Offer a direct route to Import when a report has no entries.
        var button = new javafx.scene.control.Button("Import your first text  →");
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> { if (importNavigator != null) importNavigator.run(); });
        box.getChildren().addAll(glyph, heading, hint, button);
        return box;
    }
    // Provides stored report data from the current workspace.
    private DataService dataService;

    /**
     * Configures the report tables and maps data fields to their columns.
     *
     * @param location FXML location, if known
     * @param resources FXML resource bundle, if provided
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Initialize the import and sentence history tables with empty
     * states, flexible column sizing, and value factories for names, counts, timestamps,
     * algorithms, and duplicate status.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Give each table guidance appropriate to its missing history.
        filesTable.setPlaceholder(emptyState("▤", "Your next chapter starts here", "Import a text file to see its word counts and import history in this workspace."));
        sentencesTable.setPlaceholder(emptyState("✦", "A blank page, full of potential", "Import a corpus, then open Generate to create your first sentences."));
        // Resize columns to fit available table width, preserving the final flexible column.
        filesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Apply the same flexible-width behavior to the generated-sentence table.
        sentencesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Map imported file metadata to the corresponding report columns.
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

        // Map sentence text and generation metadata to the history columns.
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

    /**
     * Stores the service used to read report data.
     *
     * @param dataService active in-memory or MySQL-backed service
     *
     * This method was created by ChatGPT codex
     * Prompt: Inject the active DataService so Reports can read history and
     * corpus totals from the current workspace.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Receives the window through the shared controller interface.
     *
     * @param stage host window supplied by the main controller
     *
     * This method was created by ChatGPT codex
     * Prompt: Implement the host-stage hook for a reports screen that opens
     * no dialogs.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setHostStage(Stage stage) {
        // Not needed.
    }

    /**
     * Receives the shared status callback through the controller interface.
     *
     * @param refresher shared application status callback
     *
     * This method was created by ChatGPT codex
     * Prompt:Implement the status callback hook without storing the callback
     * because the reports screen only reads corpus data.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setStatusRefresher(Runnable refresher) {
        // Reports are read-only.
    }

    /**
     * Reloads both histories and updates the displayed corpus statistics.
     *
     * This method was created by ChatGPT codex
     * Prompt:Fetch import and sentence history from the active service,
     * replace both table lists, and update corpus totals, duplicate count, and the latest
     * import label.
     *
     * Written by: Ahmed Sherwani(AJS170005) and Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void refresh() {
        // Wait for the shared service before loading the reports.
        if (dataService == null) {
            return;
        }
        List<ImportedFile> files = dataService.listImportedFiles();
        List<GeneratedSentence> sentences = dataService.listSentences();

        // Replace both tables with the current history snapshots.
        filesTable.setItems(FXCollections.observableArrayList(files));
        sentencesTable.setItems(FXCollections.observableArrayList(sentences));

        // Count duplicate flags from the same sentence history shown in the table.
        long dupes = sentences.stream().filter(GeneratedSentence::isDuplicate).count();
        statsTotal.setText(String.format("%,d", dataService.getTotalWordCount()));
        statsUnique.setText(String.format("%,d", dataService.getUniqueWordCount()));
        statsFiles.setText(String.format("%,d", files.size()));
        statsSentences.setText(String.format("%,d", sentences.size()));
        statsDuplicates.setText("Duplicate sentences: " + dupes);
        // Show a dash when the workspace has no latest import.
        statsLastImport.setText("Last import: "
                + dataService.getLastImportName().orElse("—"));
    }
}
