package edu.utdallas.cs4485.team22.sentencebuilder;

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
    private StackPane shell;
    private StartupView startup;
    private Task<DataService> initialization;
    private final PauseTransition intro = new PauseTransition(Duration.millis(850));
    private Animation reveal;
    private boolean introFinished, closed;
    private DataService readyService;

    @Override public void start(Stage stage) {
        this.stage=stage;
        Image logo=new Image(getClass().getResource("/images/utd-logo.png").toExternalForm());
        startup=new StartupView(logo,this::initializeStorage);
        shell=new StackPane(startup);
        var bounds=javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene=new Scene(shell,Math.min(1280,bounds.getWidth()-60),Math.min(900,bounds.getHeight()-100));
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/startup.css").toExternalForm());
        stage.setScene(scene);stage.setMinWidth(1040);stage.setMinHeight(600);
        stage.setTitle("Sentence Builder – UT Dallas");stage.getIcons().add(logo);
        stage.setOnCloseRequest(e->shutdown());
        stage.show();
        intro.setOnFinished(e->{introFinished=true;openWhenReady();});
        intro.play();
        initializeStorage();
    }

    /** Separate factory hook allows startup failure and slow initialization checks. */
    protected DataService createDataService() { return DataServiceFactory.create(); }

    private void initializeStorage() {
        if(closed || (initialization!=null && initialization.isRunning()))return;
        readyService=null;startup.begin();
        initialization=new Task<>() {
            @Override protected DataService call() { return createDataService(); }
        };
        initialization.setOnSucceeded(e->{
            if(closed)return;
            readyService=initialization.getValue();
            startup.setStatus("Preparing your tools…");openWhenReady();
        });
        initialization.setOnFailed(e->{
            if(closed)return;
            initialization.getException().printStackTrace();startup.showError();
        });
        Thread worker=new Thread(initialization,"workspace-startup");worker.setDaemon(true);worker.start();
    }

    private void openWhenReady() {
        if(closed || !introFinished || readyService==null)return;
        DataService service=readyService;readyService=null;
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent workspace=loader.load();workspace.setOpacity(0);
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
                transition.setOnFinished(e->{startup.stop();shell.getChildren().remove(startup);workspace.requestFocus();});
                transition.play();
            });
        } catch(Exception error) {
            // Retain a single recoverable loading surface if a view fails to initialize.
            shell.getChildren().setAll(startup);startup.setOpacity(1);
            stage.getScene().getAccelerators().clear();error.printStackTrace();startup.showError();
        }
    }
    private void shutdown() {
        closed=true;intro.stop();
        if(initialization!=null)initialization.cancel(true);
        if(reveal!=null)reveal.stop();
        if(startup!=null)startup.stop();
    }
    @Override public void stop() { shutdown(); }
    public static void main(String[] args) { launch(args); }
}
