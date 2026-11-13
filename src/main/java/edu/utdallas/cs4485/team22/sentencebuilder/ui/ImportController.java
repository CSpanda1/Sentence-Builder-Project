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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Controller for the import screen (file chooser + background progress).
 *
 * @author Ved Mahant
 * @author Ahmed Sherwani
 * @date 2026-10-16
 */
public class ImportController implements MainController.ServiceAware, MainController.Refreshable {

    private static final String PROJECT_BOOK_NAME = "pg11.txt";

    @FXML private TextField pathField;
    @FXML private Button browseButton;
    @FXML private Button projectBookButton;
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
        Path dataDir = Path.of(System.getProperty("user.dir"), "data");
        if (Files.isDirectory(dataDir)) {
            chooser.setInitialDirectory(dataDir.toFile());
        }
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectPath(file.toPath());
        }
    }

    /**
     * Selects the team Project Gutenberg book ({@code pg11.txt} – Alice).
     *
     * @author Ahmed Sherwani
     * @author Nirav Rohra
     * @date 2026-11-13
     */
    @FXML
    public void onUseProjectBook() {
        try {
            Path book = resolveProjectBook();
            selectPath(book);
            appendLog("Project book ready: Alice's Adventures in Wonderland ("
                    + PROJECT_BOOK_NAME + ")");
        } catch (Exception ex) {
            pathField.getStyleClass().add("field-error");
            progressLabel.setText("Could not find " + PROJECT_BOOK_NAME);
            appendLog("Project book error: " + ex.getMessage());
        }
    }

    private void selectPath(Path path) {
        selectedPath = path;
        pathField.setText(selectedPath.toAbsolutePath().toString());
        pathField.getStyleClass().remove("field-error");
        appendLog("Selected: " + selectedPath.getFileName());
    }

    /**
     * Finds {@code data/pg11.txt} next to the working directory, or copies it
     * from the classpath resource if needed.
     *
     * @return path to the project book
     * @throws Exception if the book cannot be located
     * @author Nirav Rohra
     * @date 2026-11-13
     */
    private Path resolveProjectBook() throws Exception {
        Path[] candidates = {
                Path.of(System.getProperty("user.dir"), "data", PROJECT_BOOK_NAME),
                Path.of(System.getProperty("user.dir"), PROJECT_BOOK_NAME),
                Path.of("data", PROJECT_BOOK_NAME)
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        // Fall back to classpath copy into a temp file.
        try (InputStream in = getClass().getResourceAsStream("/data/" + PROJECT_BOOK_NAME)) {
            if (in == null) {
                throw new IllegalStateException(
                        PROJECT_BOOK_NAME + " not found under data/ or classpath /data/");
            }
            Path temp = Files.createTempFile("pg11-", ".txt");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp;
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
        if (projectBookButton != null) {
            projectBookButton.setDisable(busy);
        }
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
