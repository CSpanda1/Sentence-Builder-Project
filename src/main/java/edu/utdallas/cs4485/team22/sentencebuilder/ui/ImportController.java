package edu.utdallas.cs4485.team22.sentencebuilder.ui;

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
 * @author Ved Mahant
 * @date 2026-10-16
 */
public class ImportController implements MainController.ServiceAware, MainController.Refreshable {

    @FXML private TextField pathField;
    @FXML private Button browseButton;
    @FXML private Button importButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private TextArea logArea;

    private DataService dataService;
    private Stage stage;
    private Runnable statusRefresher;
    private Path selectedPath;
    private Task<ImportedFile> runningTask;

    @Override
    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void setHostStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void setStatusRefresher(Runnable refresher) {
        this.statusRefresher = refresher;
    }

    @Override
    public void refresh() {
        // Nothing to reload; status is owned by MainController.
    }

    /**
     * Opens a file chooser restricted to plain-text files.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select text file to import");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files", "*.txt", "*.text"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPath = file.toPath();
            pathField.setText(selectedPath.toAbsolutePath().toString());
            pathField.getStyleClass().remove("field-error");
            appendLog("Selected: " + selectedPath.getFileName());
        }
    }

    /**
     * Starts a background import Task and binds the progress bar.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onImport() {
        if (selectedPath == null) {
            pathField.getStyleClass().add("field-error");
            progressLabel.setText("Please choose a .txt file first.");
            appendLog("Validation: no file selected.");
            return;
        }
        if (runningTask != null && runningTask.isRunning()) {
            return;
        }

        Path path = selectedPath;
        Task<ImportedFile> task = new Task<>() {
            @Override
            protected ImportedFile call() throws Exception {
                return dataService.importText(path, progress -> {
                    updateProgress(progress, 1.0);
                    updateMessage(String.format("Importing… %.0f%%", progress * 100));
                });
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        setBusy(true);
        runningTask = task;

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
            if (statusRefresher != null) {
                statusRefresher.run();
            }
        });

        task.setOnFailed(e -> {
            unbindProgress();
            Throwable err = task.getException();
            String msg = err != null ? err.getMessage() : "Unknown error";
            progressBar.setProgress(0);
            progressLabel.setText("Import failed.");
            appendLog("Failed: " + msg);
            setBusy(false);
        });

        task.setOnCancelled(e -> {
            unbindProgress();
            dataService.cancelImport();
            progressBar.setProgress(0);
            progressLabel.setText("Import cancelled.");
            appendLog("Cancelled by user.");
            setBusy(false);
        });

        Thread thread = new Thread(task, "import-worker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Requests cancellation of the running import.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    @FXML
    public void onCancel() {
        if (runningTask != null && runningTask.isRunning()) {
            dataService.cancelImport();
            runningTask.cancel(true);
        }
    }

    private void setBusy(boolean busy) {
        importButton.setDisable(busy);
        browseButton.setDisable(busy);
        cancelButton.setDisable(!busy);
    }

    private void unbindProgress() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
    }

    private void appendLog(String line) {
        if (logArea.getText() == null || logArea.getText().isEmpty()) {
            logArea.setText(line);
        } else {
            logArea.appendText("\n" + line);
        }
    }
}
