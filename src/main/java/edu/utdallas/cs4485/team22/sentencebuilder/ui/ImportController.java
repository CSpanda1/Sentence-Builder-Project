package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * ImportController.java
 *
 * Controls the JavaFX corpus import screen, lets the user select a text
 * file, and runs the import in a background task through DataService.
 * It displays progress and records completion, failure, or cancellation.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.model.ImportedFile;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

/**
 * Controller for the import screen (file chooser + background progress).
 *
 * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public class ImportController implements MainController.ServiceAware, MainController.Refreshable {

    // Displays the selected file path and highlights a missing selection.
    @FXML private TextField pathField;
    // Opens the file chooser when no import is active.
    @FXML private Button browseButton;
    // Starts an import after a file has been selected.
    @FXML private Button importButton;
    // Requests cancellation of the active import.
    @FXML private Button cancelButton;
    // Shows the worker's reported completion fraction.
    @FXML private ProgressBar progressBar;
    // Displays import progress or the final outcome.
    @FXML private Label progressLabel;
    // Keeps a visible log of selections and import outcomes.
    @FXML private TextArea logArea;

    // Provides the import and cancellation operations.
    private DataService dataService;
    // Owns the file chooser dialog.
    private Stage stage;
    // Updates shared corpus totals after a successful import.
    private Runnable statusRefresher;
    // Stores the file chosen for the next import.
    private Path selectedPath;
    // Identifies the worker that can be cancelled.
    private Task<ImportedFile> runningTask;

    /**
     * Stores the service that imports corpus text and handles cancellation.
     *
     * @param dataService active in-memory or MySQL-backed service
     *
     * This method was created by ChatGPT codex
     * Prompt:Store the active DataService so the controller can import text
     * and request cancellation.
     *
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Stores the owner window for the file chooser.
     *
     * @param stage host window supplied by the main controller
     *
     * This method was created by ChatGPT codex
     * Prompt: Store the host stage so the file chooser opens with the correct
     * owner.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setHostStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Registers the callback used to update application totals after an import.
     *
     * @param refresher application status callback
     *
     * This method was created by ChatGPT codex
     * Prompt: Register a callback that updates the shared status display when
     * an import succeeds.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void setStatusRefresher(Runnable refresher) {
        this.statusRefresher = refresher;
    }

    /**
     * Keeps the existing file selection and import log when the view is refreshed.
     *
     * This method was created by ChatGPT codex
     * Prompt: Implement the view refresh hook while retaining the current
     * file selection and import log.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override
    public void refresh() {
        // Nothing to reload; status is owned by MainController.
    }

    /**
     * Opens a file chooser restricted to plain-text files.
     *
     * This method was created by ChatGPT codex
     * Prompt: Open a plain-text file chooser and update the selected path,
     * import button, and log when a file is chosen.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select text file to import");
        // Offer plain-text files in the chooser.
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files", "*.txt", "*.text"));
        // Leave the previous selection available if the chooser is dismissed.
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPath = file.toPath();
            pathField.setText(selectedPath.toAbsolutePath().toString());
            pathField.getStyleClass().remove("field-error");
            appendLog("Selected: " + selectedPath.getFileName());
            importButton.setDisable(false);
        }
    }

    /**
     * Starts a background import Task and binds the progress bar.
     *
     *
     * This method was created by ChatGPT codex
     * Prompt: Validate the selected file, import it in a background JavaFX
     * task, and update the controls for progress, success, failure, or cancellation.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onImport() {
        // Require a file before trying to create an import task.
        if (selectedPath == null) {
            pathField.getStyleClass().add("field-error");
            progressLabel.setText("Please choose a .txt file first.");
            appendLog("Validation: no file selected.");
            return;
        }
        // Ignore repeated import requests while the current task is running.
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }

        // Capture the selected path for the worker and report progress through the task.
        Path path = selectedPath;
        Task<ImportedFile> task = new Task<>() {
            @Override
            protected ImportedFile call() throws Exception {
                // Publish the service's progress through the JavaFX task.
                return dataService.importText(path, progress -> {
                    updateProgress(progress, 1.0);
                    updateMessage(String.format("Importing… %.0f%%", progress * 100));
                });
            }
        };

        // Bind the view to task updates while the import runs.
        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        setBusy(true);
        runningTask = task;

        // Show the import summary and refresh the shared corpus totals.
        task.setOnSucceeded(e -> {
            unbindProgress();
            ImportedFile record = task.getValue();
            progressBar.setProgress(1.0);
            progressLabel.setText("Import complete.");
            appendLog(String.format(
                    "Imported %s – %d tokens, %d unique words.",
                    record.getFileName(),
                    record.getWordCount(),
                    record.getUniqueWordCount()));
            setBusy(false);
            // Refresh the shared totals after the imported file has been recorded.
            if (statusRefresher != null) {
                statusRefresher.run();
            }
        });

        // Restore the controls and log the worker error.
        task.setOnFailed(e -> {
            unbindProgress();
            Throwable err = task.getException();
            String msg = err != null ? err.getMessage() : "Unknown error";
            progressBar.setProgress(0);
            progressLabel.setText("Import failed.");
            appendLog("Failed: " + msg);
            setBusy(false);
        });

        // Release the progress bindings and reset the view after cancellation.
        task.setOnCancelled(e -> {
            unbindProgress();
            dataService.cancelImport();
            progressBar.setProgress(0);
            progressLabel.setText("Import cancelled.");
            appendLog("Cancelled by user.");
            setBusy(false);
        });

        // Run file and corpus work off the JavaFX application thread.
        Thread thread = new Thread(task, "import-worker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Requests cancellation of the running import.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Signal cancellation to the data service and interrupt the
     * active JavaFX import task.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void onCancel() {
        if (runningTask != null && runningTask.isRunning()) {
            dataService.cancelImport();
            // Interrupt the worker in addition to setting the service cancellation flag.
            runningTask.cancel(true);
        }
    }

    /**
     * Updates which controls are available while an import runs.
     *
     * @param busy whether an import is in progress
     *
     * This method was created by ChatGPT codex
     * Prompt:  Update the import, browse, and cancel buttons based on whether
     * a worker is running and a file is selected.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void setBusy(boolean busy) {
        // Require a file selection and an idle worker before enabling Import.
        importButton.setDisable(busy || selectedPath == null);
        browseButton.setDisable(busy);
        cancelButton.setDisable(!busy);
    }

    /**
     * Detaches the task bindings so completion handlers can display a final status.
     *
     * This method was created by ChatGPT codex
     * Prompt:  Detach the progress bar and label from task properties so
     * completion handlers can set their final values.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void unbindProgress() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
    }

    /**
     * Adds an entry to the import log, starting a new line when needed.
     *
     * @param line log message to append
     *
     * This method was created by ChatGPT codex
     * Prompt:  Append an import log entry on a new line when the log already
     * contains text.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void appendLog(String line) {
        if (logArea.getText() == null || logArea.getText().isEmpty()) {
            logArea.setText(line);
        } else {
            logArea.appendText("\n" + line);
        }
    }
}
