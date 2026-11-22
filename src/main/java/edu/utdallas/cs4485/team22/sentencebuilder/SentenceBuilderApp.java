package edu.utdallas.cs4485.team22.sentencebuilder;

/*****************************************************************************************************************
 * SentenceBuilderApp.java
 *
 * Launches the JavaFX application and displays a loading screen while
 * the data service initializes. It opens the workspace when ready,
 * offers retry after startup errors, and stops startup work at exit.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.service.*;
import edu.utdallas.cs4485.team22.sentencebuilder.ui.MainController;
import edu.utdallas.cs4485.team22.sentencebuilder.ui.StartupView;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Starts immediately with a branded loading screen while storage initializes. */
public class SentenceBuilderApp extends Application {
    private Stage stage;
    // Holds the startup view and the workspace during the crossfade.
    private StackPane shell;
    private StartupView startup;
    // Tracks the current storage initialization attempt.
    private Task<DataService> initialization;
    // Keeps the loading screen visible for a minimum intro period.
    private final PauseTransition intro = new PauseTransition(Duration.millis(850));
    // Tracks the crossfade so shutdown can stop it.
    private Animation reveal;
    // Tracks whether the intro has finished and whether the app is closing.
    private boolean introFinished, closed;
    // Holds the initialized service until the workspace can open.
    private DataService readyService;

    /**
     * Shows the startup view and begins initializing workspace storage.
     *
     * @param stage primary window supplied by JavaFX
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Configure and show the primary JavaFX window with the loading
     * screen, then start the intro timer and background storage initialization.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override public void start(Stage stage) {
        this.stage=stage;
        Image logo=new Image(getClass().getResource("/images/utd-logo.png").toExternalForm());
        startup=new StartupView(logo,this::initializeStorage);
        shell=new StackPane(startup);
        // Size the scene using the primary screen's usable area.
        var bounds=javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene=new Scene(shell,Math.min(1280,bounds.getWidth()-60),Math.min(900,bounds.getHeight()-100));
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/startup.css").toExternalForm());
        stage.setScene(scene);stage.setMinWidth(1040);stage.setMinHeight(600);
        stage.setTitle("Sentence Builder – UT Dallas");stage.getIcons().add(logo);
        stage.setOnCloseRequest(e->shutdown());
        stage.show();
        // Recheck readiness when the intro timer finishes.
        intro.setOnFinished(e->{introFinished=true;openWhenReady();});
        intro.play();
        initializeStorage();
    }

    /**
     * Creates the workspace data service through an overridable factory hook.
     *
     * @return the data service for the workspace
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Provide a separate factory method for creating the data service
     * so startup can be exercised with slow or failing initialization.
     *
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    protected DataService createDataService() { return DataServiceFactory.create(); }

    /**
     * Starts or retries storage initialization on a background thread.
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Prevent overlapping attempts, create the data service in a
     * JavaFX task, and update the startup view on success or failure.
     *
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void initializeStorage() {
        // | (initialization!=null|Skip new attempts after shutdown or while a worker is already running.
        if(closed || (initialization!=null && initialization.isRunning()))return;
        readyService=null;startup.begin();
        initialization=new Task<>() {
            @Override protected DataService call() { return createDataService(); }
        };
        // Keep the service ready and open the workspace if the intro has finished.
        initialization.setOnSucceeded(e->{
            if(closed)return;
            readyService=initialization.getValue();
            startup.setStatus("Preparing your tools…");openWhenReady();
        });
        // Report the failure in the terminal and show the retry state.
        initialization.setOnFailed(e->{
            if(closed)return;
            initialization.getException().printStackTrace();startup.showError();
        });
        // Initialize storage off the JavaFX application thread.
        Thread worker=new Thread(initialization,"workspace-startup");worker.setDaemon(true);worker.start();
    }

    /**
     * Loads and reveals the workspace once initialization and the intro delay are complete.
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Wait for storage and the intro timer, load the main FXML view
     * with the ready service, and crossfade from the startup screen. Restore the retry screen
     * if the workspace fails to load.
     *
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void openWhenReady() {
        if(closed || !introFinished || readyService==null)return;
        // Consume the ready service once so a second readiness callback cannot reopen the workspace.
        DataService service=readyService;readyService=null;
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent workspace=loader.load();workspace.setOpacity(0);
            // Attach the workspace behind the loading view before fading it in.
            shell.getChildren().add(0,workspace);
            MainController controller=loader.getController();controller.init(service,stage);
            String mode=service instanceof JdbcDataService?"MySQL":"Demo - changes are temporary";
            stage.setTitle("Sentence Builder – Team 22 ("+mode+")");
            startup.setStatus("Your workspace is ready.");
            // Give the newly attached controls a layout pulse before the crossfade.
            Platform.runLater(()->{
                if(closed)return;
                FadeTransition out=new FadeTransition(Duration.millis(280),startup);out.setFromValue(1);out.setToValue(0);
                FadeTransition in=new FadeTransition(Duration.millis(280),workspace);in.setFromValue(0);in.setToValue(1);
                ParallelTransition transition=new ParallelTransition(out,in);reveal=transition;
                transition.setInterpolator(Interpolator.EASE_BOTH);
                // Stop the startup animation, remove its view, and focus the workspace after the crossfade.
                transition.setOnFinished(e->{startup.stop();shell.getChildren().remove(startup);workspace.requestFocus();});
                transition.play();
            });
        } catch(Exception error) {
            // Retain a single recoverable loading surface if a view fails to initialize.
            shell.getChildren().setAll(startup);startup.setOpacity(1);
            stage.getScene().getAccelerators().clear();error.printStackTrace();startup.showError();
        }
    }
    /**
     * Cancels startup work and stops animations when the app closes.
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Mark the app as closed, cancel any pending initialization, and
     * stop the intro timer, crossfade, and loading animation.
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private void shutdown() {
        // Mark the application as closed before cancelling pending work.
        closed=true;intro.stop();
        if(initialization!=null)initialization.cancel(true);
        if(reveal!=null)reveal.stop();
        if(startup!=null)startup.stop();
    }
    /**
     * Runs shutdown cleanup when JavaFX stops the application.
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Override the JavaFX shutdown hook to release startup tasks and
     * animations through the common cleanup method.
     *Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.Written by: Ved Mahant and Nirav Rohra at the University of Texas at Dallas starting October 16th, 2026.
     */
    @Override public void stop() { shutdown(); }
    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments for the application
     *
     * This method was created by ChatGPT codex and modified
     * Prompt:  Provide a standard Java entry point that forwards command-line
     * arguments to JavaFX.
     *
     *
     * Written by: Ved Mahant(VXM220098) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public static void main(String[] args) { launch(args); }
}
