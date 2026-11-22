package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * MainController.java
 *
 * Coordinates the JavaFX main window, sidebar navigation, and shortcuts.
 * Loads feature screens with a shared data service and refreshes corpus
 * totals in the status bar.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

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
 * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026
 */
public class MainController {

    // Holds the currently displayed feature screen.
    @FXML private StackPane contentPane;
    // Contains the sidebar navigation controls.
    @FXML private javafx.scene.layout.VBox sidebar;
    // Sidebar controls select cached workspace screens.
    @FXML private Button navHome;
    // Displays the title of the currently open workspace screen.
    @FXML private Label pageTitle;
    @FXML private Button navImport;
    @FXML private Button navGenerate;
    @FXML private Button navAutocomplete;
    @FXML private Button navWords;
    @FXML private Button navReports;
    // Shows the storage mode for the active workspace.
    @FXML private Label modeBadge;
    // Displays the corpus totals and the most recent import in the status bar.
    @FXML private Label statusWords;
    @FXML private Label statusUnique;
    @FXML private Label statusLastImport;

    // Provides access to the active in-memory or JDBC data service.
    private DataService dataService;
    // References the main application window for dialogs and screen operations.
    private Stage stage;
    // Caches views and controllers by their navigation key.
    private final Map<String, Node> views = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();
    // Tracks the sidebar button for the currently active screen.
    private Button activeNav;

    /**
     * Injects the shared data service and registers keyboard shortcuts.
     *
     * @param dataService application data service (stub or JDBC)
     * @param stage       primary stage (for file dialogs)
     *
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Initialize the workspace with the active data service,
     * configure the mode badge and sidebar layout, load the views, show Overview, and
     * register shortcuts.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void init(DataService dataService, Stage stage) {
        // Store the shared data service and main window for later controller operations.
        this.dataService = dataService;
        this.stage = stage;
        // Identify whether the application is using MySQL or the demo workspace.
        modeBadge.setText(dataService instanceof edu.utdallas.cs4485.team22.sentencebuilder.service.JdbcDataService
                ? "●  MYSQL CONNECTED" : "●  DEMO WORKSPACE");
        // Hide the sidebar note on shorter windows and let the sidebar reclaim its space.
        Node tip = sidebar.lookup(".sidebar-note");
        // Hide the sidebar note while the window is too short to display it.
        tip.visibleProperty().bind(stage.getScene().heightProperty().greaterThanOrEqualTo(740));
        // Remove the hidden note from layout so the sidebar can reclaim its space.
        tip.managedProperty().bind(tip.visibleProperty());
        // Create and cache all workspace views.
        loadAllViews();
        // Load current corpus totals and the latest import into the status bar.
        refreshStatus();
        // Open the Overview screen when initialization finishes.
        showHome();
        // Register application-wide keyboard shortcuts.
        registerShortcuts();
    }

    /**
     * Preloads each feature FXML so navigation is instant.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Build the Overview with navigation callbacks and preload the
     * feature screens so switching views can use the cached nodes.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void loadAllViews() {
        // Build the Overview in Java and pass it actions for opening each tool.
        HomeController home = new HomeController(dataService, this::showImport, this::showGenerate,
                this::showAutocomplete, this::showWords, this::showReports);
        // Place the Overview UI in a scrollable pane.
        javafx.scene.control.ScrollPane welcome = new javafx.scene.control.ScrollPane(home.build());
        // Make the Overview fill the available pane width and height.
        welcome.setFitToWidth(true); welcome.setFitToHeight(true);
        // Apply the shared scrolling-screen CSS style.
        welcome.getStyleClass().add("screen-scroll");
        // Cache the Overview node and controller for later refreshes and navigation.
        views.put("home", welcome); controllers.put("home", home);
        // Preload the FXML-based screens after registering Overview.
        loadView("import", "/fxml/ImportView.fxml");
        loadView("generate", "/fxml/GenerationView.fxml");
        loadView("autocomplete", "/fxml/AutocompleteView.fxml");
        loadView("words", "/fxml/WordEditorView.fxml");
        loadView("reports", "/fxml/ReportsView.fxml");
    }

    /**
     * Loads and caches a feature view together with its controller.
     *
     * @param key navigation key used to look up the view
     * @param resource classpath location of the FXML file
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Load an FXML resource, inject shared dependencies into its
     * controller when supported, and cache the scrollable view under its navigation key.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void loadView(String key, String resource) {
        try {
            // Create an FXML loader for the requested feature resource.
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            // Load the JavaFX node and retrieve its associated controller.
            Node node = loader.load();
            Object controller = loader.getController();
            // Let Reports open Import and supply shared dependencies to compatible screens.
            if (controller instanceof ReportsController reports) reports.setImportNavigator(this::showImport);
            // Inject shared dependencies into controllers that support them.
            if (controller instanceof ServiceAware aware) {
                aware.setDataService(dataService);
                aware.setHostStage(stage);
                aware.setStatusRefresher(this::refreshStatus);
            }
            // Wrap each feature screen so it stays accessible in a smaller window.
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(node);
            // Resize the wrapped node to fill the available scroll-pane area.
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.getStyleClass().add("screen-scroll");
            // Set a minimum feature height for adequate content visibility.
            if (node instanceof javafx.scene.layout.Region region) {
                region.setMinHeight(key.equals("reports") ? 740 : 720);
            }
            // Cache the loaded view and controller for navigation and refresh.
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
     * This method was created and helped by ChatGPT codex
     * Prompt: Read corpus totals and the latest import in a background task.
     * Apply the result or report a failure only if the request is still current.
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void refreshStatus() {
        // Identify the newest refresh so an older task cannot overwrite the status bar.
        final long ticket=++statusRevision;
        // Run corpus-stat retrieval in a background JavaFX task.
        javafx.concurrent.Task<StatusSnapshot> task=new javafx.concurrent.Task<>() {
            // Read total words, unique words, and the latest import name.
            @Override protected StatusSnapshot call() {
                return new StatusSnapshot(dataService.getTotalWordCount(),dataService.getUniqueWordCount(),
                        dataService.getLastImportName().orElse("—"));
            }
        };
        // Apply the latest status snapshot on the JavaFX application thread.
        task.setOnSucceeded(e->{
            // Ignore results from older refresh requests.
            if(ticket!=statusRevision)return;
            // Copy the retrieved snapshot into the status-bar labels.
            StatusSnapshot snapshot=task.getValue();
            statusWords.setText("Words: "+snapshot.total());
            statusUnique.setText("Unique: "+snapshot.unique());
            statusLastImport.setText("Last import: "+snapshot.lastImport());
        });
        task.setOnFailed(e->{
            // Show a database-related failure only for the most recent refresh.
            if(ticket==statusRevision)statusLastImport.setText("Status unavailable — check database connection");
        });
        // Read corpus statistics off the JavaFX application thread.
        Thread worker=new Thread(task,"workspace-status");worker.setDaemon(true);worker.start();
    }
    private long statusRevision;
    private record StatusSnapshot(int total,int unique,String lastImport) {}

    /**
     * Registers platform shortcut combinations for sidebar navigation.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Map the platform shortcut key plus digits zero through five to
     * Overview and the five feature screens.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void registerShortcuts() {
        // Use the platform shortcut modifier for navigation accelerators.
        KeyCombination.Modifier mod = KeyCombination.SHORTCUT_DOWN;
        // Map Command/Ctrl+0 to the Overview screen.
        stage.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, mod), this::showHome);
        // Map Command/Ctrl+1 to the Import screen.
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT1, mod), this::showImport);
        // Map Command/Ctrl+2 to the Generate screen.
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT2, mod), this::showGenerate);
        // Map Command/Ctrl+3 to the Autocomplete screen.
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT3, mod), this::showAutocomplete);
        // Map Command/Ctrl+4 to the Word Editor screen.
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT4, mod), this::showWords);
        // Map Command/Ctrl+5 to the Reports screen.
        stage.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DIGIT5, mod), this::showReports);
    }

    /**
     * Displays a cached screen and updates the active navigation state.
     *
     * @param key navigation key of the cached view
     * @param nav sidebar button associated with the screen
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Swap in the cached view, reset its scroll position, highlight
     * its sidebar button, update the page title, and refresh its controller when supported.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void show(String key, Button nav) {
        contentPane.getChildren().setAll(views.get(key));
        // Reset the scroll position after the displayed view is attached.
        if (views.get(key) instanceof javafx.scene.control.ScrollPane scroll) {
            javafx.application.Platform.runLater(() -> scroll.setVvalue(0));
        }
        // Move the active highlight to the newly selected sidebar button.
        if (activeNav != null) {
            activeNav.getStyleClass().remove("nav-active");
        }
        nav.getStyleClass().add("nav-active");
        activeNav = nav;
        pageTitle.setText(nav.getText());
        // Refresh feature data when the displayed controller supports it.
        Object c = controllers.get(key);
        if (c instanceof Refreshable r) {
            r.refresh();
        }
    }

    /**
     * Opens the overview screen.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Route the Overview action through the shared navigation helper
     * with the overview view key and sidebar button.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML public void showHome() { show("home", navHome); }

    /**
     * Displays the getting-started guide for the current workspace.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Show an information dialog with instructions for importing,
     * generating, and composing, along with keyboard shortcuts and the current storage mode.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML public void showHelp() {
        // Create an information dialog for the getting-started guide.
        javafx.scene.control.Alert help = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        // Attach the dialog to the main window and set its title and header.
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

    /**
     * Opens the import screen.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt: Route the Import corpus action through the shared navigation
     * helper.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void showImport() {
        show("import", navImport);
    }

    /**
     * Opens the generation screen.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Route the Generate action through the shared navigation helper.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void showGenerate() {
        show("generate", navGenerate);
    }

    /**
     * Opens the autocomplete screen.
     * This method was created and helped by ChatGPT codex
     * Prompt:  Route the Compose action through the shared navigation helper.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void showAutocomplete() {
        show("autocomplete", navAutocomplete);
    }

    /**
     * Opens the word editor.
     * This method was created and helped by ChatGPT codex
     * Prompt:  Route the Word library action through the shared navigation
     * helper.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void showWords() {
        show("words", navWords);
    }

    /**
     * Opens the reports screen.
     * This method was created and helped by ChatGPT codex
     * Prompt:  Route the Reports action through the shared navigation helper.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @FXML
    public void showReports() {
        show("reports", navReports);
    }

    /**
     * Controllers that need the shared data service implement this.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public interface ServiceAware {
        void setDataService(DataService dataService);

        void setHostStage(Stage stage);

        void setStatusRefresher(Runnable refresher);
    }

    /**
     * Controllers that reload tables when shown implement this.
     *
     * Written by: Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public interface Refreshable {
        void refresh();
    }
}
