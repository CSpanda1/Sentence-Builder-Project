package edu.utdallas.cs4485.team22.sentencebuilder;

import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataServiceFactory;
import edu.utdallas.cs4485.team22.sentencebuilder.service.JdbcDataService;
import edu.utdallas.cs4485.team22.sentencebuilder.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application entry point for the Sentence Builder JavaFX UI.
 *
 * <p>Loads the main window and injects the shared {@link DataService}
 * chosen by {@link DataServiceFactory} (JDBC when MySQL is up, else stub).</p>
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public class SentenceBuilderApp extends Application {

    private final DataService dataService = DataServiceFactory.create();

    /**
     * Starts the primary stage and shows the main window.
     *
     * @param stage primary JavaFX stage
     * @throws Exception if FXML cannot be loaded
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm());

        String mode = dataService instanceof JdbcDataService ? "MySQL" : "Stub";
        stage.setTitle("Sentence Builder – Team 22 (" + mode + ")");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        // Init after the scene exists so keyboard accelerators can register.
        MainController controller = loader.getController();
        controller.init(dataService, stage);

        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments (unused)
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public static void main(String[] args) {
        launch(args);
    }
}
