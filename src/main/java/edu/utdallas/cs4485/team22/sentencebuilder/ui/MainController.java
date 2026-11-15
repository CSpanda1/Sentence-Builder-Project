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
    @FXML private javafx.scene.layout.VBox sidebar;
    @FXML private Button navHome;
    @FXML private Label pageTitle;
    @FXML private Button navImport;
    @FXML private Button navGenerate;
    @FXML private Button navAutocomplete;
    @FXML private Button navWords;
    @FXML private Button navReports;
    @FXML private Label modeBadge;
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
        modeBadge.setText(dataService instanceof edu.utdallas.cs4485.team22.sentencebuilder.service.JdbcDataService
                ? "●  MYSQL CONNECTED" : "●  DEMO WORKSPACE");
        Node tip = sidebar.lookup(".sidebar-note");
        tip.visibleProperty().bind(stage.getScene().heightProperty().greaterThanOrEqualTo(740));
        tip.managedProperty().bind(tip.visibleProperty());
        loadAllViews();
        refreshStatus();
        showHome();
        registerShortcuts();
    }

    /**
     * Preloads each feature FXML so navigation is instant.
     *
     * @author Ved Mahant
     * @date 2026-10-16
     */
    private void loadAllViews() {
        HomeController home = new HomeController(dataService, this::showImport, this::showGenerate,
                this::showAutocomplete, this::showWords, this::showReports);
        javafx.scene.control.ScrollPane welcome = new javafx.scene.control.ScrollPane(home.build());
        welcome.setFitToWidth(true); welcome.setFitToHeight(true);
        welcome.getStyleClass().add("screen-scroll");
        views.put("home", welcome); controllers.put("home", home);
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
            if (controller instanceof ReportsController reports) reports.setImportNavigator(this::showImport);
            if (controller instanceof ServiceAware aware) {
                aware.setDataService(dataService);
                aware.setHostStage(stage);
                aware.setStatusRefresher(this::refreshStatus);
            }
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(node);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.getStyleClass().add("screen-scroll");
            if (node instanceof javafx.scene.layout.Region region) {
                region.setMinHeight(key.equals("reports") ? 740 : 720);
            }
            views.put(key, scroll);
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
        final long ticket=++statusRevision;
        javafx.concurrent.Task<StatusSnapshot> task=new javafx.concurrent.Task<>() {
            @Override protected StatusSnapshot call() {
                return new StatusSnapshot(dataService.getTotalWordCount(),dataService.getUniqueWordCount(),
                        dataService.getLastImportName().orElse("—"));
            }
        };
        task.setOnSucceeded(e->{
            if(ticket!=statusRevision)return;
            StatusSnapshot snapshot=task.getValue();
            statusWords.setText("Words: "+snapshot.total());
            statusUnique.setText("Unique: "+snapshot.unique());
            statusLastImport.setText("Last import: "+snapshot.lastImport());
        });
        task.setOnFailed(e->{
            if(ticket==statusRevision)statusLastImport.setText("Status unavailable — check database connection");
        });
        Thread worker=new Thread(task,"workspace-status");worker.setDaemon(true);worker.start();
    }
    private long statusRevision;
    private record StatusSnapshot(int total,int unique,String lastImport) {}

    private void registerShortcuts() {
        KeyCombination.Modifier mod = KeyCombination.SHORTCUT_DOWN;
        stage.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, mod), this::showHome);
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
        if (views.get(key) instanceof javafx.scene.control.ScrollPane scroll) {
            javafx.application.Platform.runLater(() -> scroll.setVvalue(0));
        }
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-active");
        }
        nav.getStyleClass().add("nav-active");
        activeNav = nav;
        pageTitle.setText(nav.getText());
        Object c = controllers.get(key);
        if (c instanceof Refreshable r) {
            r.refresh();
        }
    }

    @FXML public void showHome() { show("home", navHome); }

    @FXML public void showHelp() {
        javafx.scene.control.Alert help = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        help.initOwner(stage); help.setTitle("Getting started");
        help.setHeaderText("From a text file to your next sentence");
        help.setContentText("1. Import corpus: choose a .txt file, or choose Use sample text, then Start Import.\n\n"
            + "2. Generate: enter a known word or use Try a start word. Choose an algorithm and generate a sentence.\n\n"
            + "3. Compose: finish a word with a space, comma, or period. Press Enter or double-click to accept a suggestion.\n\n"
            + "Word library lets you filter and edit metadata; Reports shows sources and generated history.\n\n"
            + "Shortcuts: Ctrl/Cmd+0 opens Overview; Ctrl/Cmd+1–5 opens each tool.\n\n"
            + (dataService instanceof edu.utdallas.cs4485.team22.sentencebuilder.service.JdbcDataService
                ? "Storage: connected to MySQL." : "Storage: demo data is temporary and resets when you close the app."));
        help.getDialogPane().setMinWidth(510); help.showAndWait();
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
