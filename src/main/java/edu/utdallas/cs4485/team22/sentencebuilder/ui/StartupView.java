package edu.utdallas.cs4485.team22.sentencebuilder.ui;

/*****************************************************************************************************************
 * StartupView.java
 *
 * Displays the JavaFX loading screen while the workspace starts.
 * Animates the startup indicator, shows progress messages, and offers
 * a retry action when initialization fails.
 *
 * AI Assistance: ChatGPT codex was used to generate some methods
 * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Branded startup view for loading without a fixed completion percentage.
 * The application can stop the animation when it dismisses the view.
 *
 * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public final class StartupView extends StackPane {
    // Shows the current initialization step.
    private final Label status = label("Preparing your workspace…", "startup-status");
    // Provides supporting guidance for the current startup state.
    private final Label detail = label("Connecting your words, ideas, and possibilities.", "startup-detail");
    // Identifies whether startup is loading or needs attention.
    private final Label phase = label("STARTING YOUR WORKSPACE", "startup-pill");
    // Offers another startup attempt after initialization fails.
    private final Button retry = new Button("Try again");
    // Drives both arcs with one repeating animation.
    private final Timeline orbit;

    /**
     * Builds the startup layout, loading animation, and retry control.
     *
     * @param logo image displayed at the center of the indicator
     * @param retryAction callback invoked when the user requests another startup attempt
     *
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Create a JavaFX loading view with a centered university logo,
     * two counter-rotating arcs, status labels, and a retry button that starts hidden.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public StartupView(Image logo, Runnable retryAction) {
        getStyleClass().add("startup-root");
        // Draw a stationary track behind the moving arcs.
        Circle track = new Circle(88, Color.TRANSPARENT);
        track.setStroke(Color.web("#ffffff12")); track.setStrokeWidth(3);
        Arc orange = arc(88, 90, 100, "#f28a32");
        Arc green = arc(99, 210, 65, "#79ae8d");
        // Give the arcs independent rotations around the same center.
        Rotate outer = new Rotate(0,0,0), inner = new Rotate(0,0,0);
        orange.getTransforms().add(outer); green.getTransforms().add(inner);
        ImageView emblem = new ImageView(logo);
        emblem.setFitWidth(134); emblem.setFitHeight(134); emblem.setPreserveRatio(true);
        // Center the logo around the origin used by the ring.
        emblem.setX(-67); emblem.setY(-67); emblem.setAccessibleText("UT Dallas logo");
        Group mark = new Group(track, orange, green, emblem);
        StackPane ring = new StackPane(mark); ring.setMinHeight(215); ring.setPrefHeight(215);
        // Center the artwork using the ring dimensions instead of automatic layout.
        mark.setManaged(false); mark.layoutXProperty().bind(ring.widthProperty().divide(2));
        mark.layoutYProperty().bind(ring.heightProperty().divide(2));
        Label title = label("Sentence Builder", "startup-title");
        Label subtitle = label("LANGUAGE LAB  /  UT DALLAS", "startup-kicker");
        phase.setMaxWidth(Region.USE_PREF_SIZE);
        retry.getStyleClass().add("startup-retry"); retry.setOnAction(e -> retryAction.run());
        // Keep the retry control out of the initial layout.
        retry.setVisible(false); retry.setManaged(false);
        status.setAccessibleRole(javafx.scene.AccessibleRole.TEXT);
        VBox body = new VBox(16, subtitle, ring, title, phase, status, detail, retry);
        body.setAlignment(Pos.CENTER); body.setMaxWidth(620);
        getChildren().add(body);
        // Rotate the arcs in opposite directions at a constant speed.
        orbit = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(outer.angleProperty(),0), new KeyValue(inner.angleProperty(),0)),
                new KeyFrame(Duration.seconds(2.3), new KeyValue(outer.angleProperty(),360,Interpolator.LINEAR),
                        new KeyValue(inner.angleProperty(),-360,Interpolator.LINEAR)));
        orbit.setCycleCount(Animation.INDEFINITE);
    }
    /**
     * Creates an open circular arc for the loading indicator.
     *
     * @param radius horizontal and vertical radius
     * @param start starting angle in degrees
     * @param length angular extent in degrees
     * @param color CSS-compatible stroke color
     * @return configured arc
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Build a stroked JavaFX arc with the supplied radius, angles,
     * and color, using rounded ends and no fill.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private static Arc arc(double radius, double start, double length, String color) {
        Arc arc=new Arc(0,0,radius,radius,start,length);arc.setType(ArcType.OPEN);
        arc.setFill(null);arc.setStroke(Color.web(color));arc.setStrokeWidth(4);arc.setStrokeLineCap(StrokeLineCap.ROUND);return arc;
    }
    /**
     * Creates a centered label for the startup screen.
     *
     * @param text label content
     * @param style CSS class to apply
     * @return configured label
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Create a styled JavaFX label with centered text, wrapping, and
     * a maximum width.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private static Label label(String text,String style) {
        Label l=new Label(text);l.getStyleClass().add(style);l.setWrapText(true);
        l.setAlignment(Pos.CENTER); l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);l.setMaxWidth(560);return l;
    }
    /**
     * Resets the loading messages, hides the retry button, and starts the animation.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Prepare the view for a new startup attempt by restoring the
     * loading text and restarting the orbit from its first frame.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void begin() {
        phase.setText("STARTING YOUR WORKSPACE");status.setText("Preparing your workspace…");detail.setText("Connecting your words, ideas, and possibilities.");
        // Remove the previous failure action and restart the indicator.
        retry.setVisible(false);retry.setManaged(false);orbit.playFromStart();
    }
    /**
     * Updates the main startup status message.
     *
     * @param value status text to display
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Expose a method the application can call to show the current
     * initialization step.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void setStatus(String value) { status.setText(value); }
    /**
     * Stops the loading animation and shows a retry option after a startup failure.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Replace the loading text with failure guidance, reveal the
     * retry button, and move keyboard focus to it.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void showError() {
        stop();phase.setText("STARTUP NEEDS ATTENTION");status.setText("We couldn’t open your workspace.");
        detail.setText("Please try again. If it keeps happening, check the terminal for details.");
        // Make the retry control visible and ready for keyboard activation.
        retry.setManaged(true);retry.setVisible(true);retry.requestFocus();
    }
    /**
     * Stops the orbit animation.
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Expose a method the application can call to stop the indicator
     * when leaving the startup screen.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public void stop() { orbit.stop(); }
    /**
     * Reports whether the orbit animation is running.
     *
     * @return whether the orbit timeline is running
     *
     * This method was created and helped by ChatGPT codex
     * Prompt:  Check the timeline status so the caller can tell whether the
     * startup indicator is active.
     *
     * Written by:Ved Mahant(VXM220098) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public boolean isAnimating() { return orbit.getStatus()==Animation.Status.RUNNING; }
}
