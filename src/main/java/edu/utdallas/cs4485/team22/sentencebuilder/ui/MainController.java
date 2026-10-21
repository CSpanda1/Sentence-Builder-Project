package edu.utdallas.cs4485.team22.sentencebuilder.ui;

import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the main window: sidebar navigation, status bar, and shortcuts.
 *
 * @author Ved Mahant
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class MainController {

    @FXML private StackPane contentPane;
    @FXML private Button navImport;
    @FXML private Button navGenerate;
    @FXML private Button navAutocomplete;
    @FXML private Button navWords;
    @FXML private Button navReports;
    @FXML private Label statusWords;
    @FXML private Label statusUnique;
    @FXML private Label statusLastImport;

    private DataService dataService;
    private Stage stage;
    private final Map<String, Node> views = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();
    private Button activeNav;

    /**
     * Injects the shared data service and registers keyboard shortcuts.
     *
     * @param dataService application data service (stub or JDBC)
     * @param stage       primary stage (for file dialogs)
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public void init(DataService dataService, Stage stage) {
        this.dataService = dataService;
        this.stage = stage;
        loadAllViews();
        refreshStatus();
        showImport();
        registerShortcuts();
    }

    /**
     * Preloads each feature FXML so navigation is instant.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    private void loadAllViews() {
        loadView("import", "/fxml/ImportView.fxml");
        loadView("generate", "/fxml/GenerationView.fxml");
        loadView("autocomplete", "/fxml/AutocompleteView.fxml");
        loadView("words", "/fxml/WordEditorView.fxml");
        loadView("reports", "/fxml/ReportsView.fxml");
    }

    private void loadView(String key, String resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ServiceAware aware) {
                aware.setDataService(dataService);
                aware.setHostStage(stage);
                aware.setStatusRefresher(this::refreshStatus);
            }
            views.put(key, node);
            controllers.put(key, controller);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resource, e);
        }
    }

    /**
     * Updates the status bar from the current corpus stats.
     *
     * <p>Feature screens reload themselves when shown via {@link #show}.</p>
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public void refreshStatus() {
        statusWords.setText("Words: " + dataService.getTotalWordCount());
        statusUnique.setText("Unique: " + dataService.getUniqueWordCount());
        statusLastImport.setText("Last import: "
                + dataService.getLastImportName().orElse("—"));
    }

    private void registerShortcuts() {
        KeyCombination.Modifier mod = KeyCombination.SHORTCUT_DOWN;
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT1, mod), this::showImport);
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT2, mod), this::showGenerate);
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT3, mod), this::showAutocomplete);
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT4, mod), this::showWords);
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT5, mod), this::showReports);
    }

    private void show(String key, Button nav) {
        contentPane.getChildren().setAll(views.get(key));
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-active");
        }
        nav.getStyleClass().add("nav-active");
        activeNav = nav;
        Object c = controllers.get(key);
        if (c instanceof Refreshable r) {
            r.refresh();
        }
    }

    /** Opens the import screen. @author Ved Mahant @date 2026-10-16 */
    @FXML
    public void showImport() {
        show("import", navImport);
    }

    /** Opens the generation screen. @author Ved Mahant @date 2026-10-16 */
    @FXML
    public void showGenerate() {
        show("generate", navGenerate);
    }

    /** Opens the autocomplete screen. @author Ved Mahant @date 2026-10-16 */
    @FXML
    public void showAutocomplete() {
        show("autocomplete", navAutocomplete);
    }

    /** Opens the word editor. @author Ved Mahant @date 2026-10-16 */
    @FXML
    public void showWords() {
        show("words", navWords);
    }

    /** Opens the reports screen. @author Ved Mahant @date 2026-10-16 */
    @FXML
    public void showReports() {
        show("reports", navReports);
    }

    /**
     * Controllers that need the shared data service implement this.
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public interface ServiceAware {
        void setDataService(DataService dataService);

        void setHostStage(Stage stage);

        void setStatusRefresher(Runnable refresher);
    }

    /**
     * Controllers that reload tables when shown implement this.
     *
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public interface Refreshable {
        void refresh();
    }
}
