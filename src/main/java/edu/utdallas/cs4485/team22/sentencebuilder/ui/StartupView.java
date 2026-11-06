package edu.utdallas.cs4485.team22.sentencebuilder.ui;

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

/** Branded indeterminate startup indicator. All motion stops when it is dismissed. */
public final class StartupView extends StackPane {
    private final Label status = label("Preparing your workspace…", "startup-status");
    private final Label detail = label("Connecting your words, ideas, and possibilities.", "startup-detail");
    private final Label phase = label("STARTING YOUR WORKSPACE", "startup-pill");
    private final Button retry = new Button("Try again");
    private final Timeline orbit;

    public StartupView(Image logo, Runnable retryAction) {
        getStyleClass().add("startup-root");
        Circle track = new Circle(88, Color.TRANSPARENT);
        track.setStroke(Color.web("#ffffff12")); track.setStrokeWidth(3);
        Arc orange = arc(88, 90, 100, "#f28a32");
        Arc green = arc(99, 210, 65, "#79ae8d");
        Rotate outer = new Rotate(0,0,0), inner = new Rotate(0,0,0);
        orange.getTransforms().add(outer); green.getTransforms().add(inner);
        ImageView emblem = new ImageView(logo);
        emblem.setFitWidth(134); emblem.setFitHeight(134); emblem.setPreserveRatio(true);
        emblem.setX(-67); emblem.setY(-67); emblem.setAccessibleText("UT Dallas logo");
        Group mark = new Group(track, orange, green, emblem);
        StackPane ring = new StackPane(mark); ring.setMinHeight(215); ring.setPrefHeight(215);
        mark.setManaged(false); mark.layoutXProperty().bind(ring.widthProperty().divide(2));
        mark.layoutYProperty().bind(ring.heightProperty().divide(2));
        Label title = label("Sentence Builder", "startup-title");
        Label subtitle = label("LANGUAGE LAB  /  UT DALLAS", "startup-kicker");
        phase.setMaxWidth(Region.USE_PREF_SIZE);
        retry.getStyleClass().add("startup-retry"); retry.setOnAction(e -> retryAction.run());
        retry.setVisible(false); retry.setManaged(false);
        status.setAccessibleRole(javafx.scene.AccessibleRole.TEXT);
        VBox body = new VBox(16, subtitle, ring, title, phase, status, detail, retry);
        body.setAlignment(Pos.CENTER); body.setMaxWidth(620);
        getChildren().add(body);
        orbit = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(outer.angleProperty(),0), new KeyValue(inner.angleProperty(),0)),
                new KeyFrame(Duration.seconds(2.3), new KeyValue(outer.angleProperty(),360,Interpolator.LINEAR),
                        new KeyValue(inner.angleProperty(),-360,Interpolator.LINEAR)));
        orbit.setCycleCount(Animation.INDEFINITE);
    }
    private static Arc arc(double radius, double start, double length, String color) {
        Arc arc=new Arc(0,0,radius,radius,start,length);arc.setType(ArcType.OPEN);
        arc.setFill(null);arc.setStroke(Color.web(color));arc.setStrokeWidth(4);arc.setStrokeLineCap(StrokeLineCap.ROUND);return arc;
    }
    private static Label label(String text,String style) {
        Label l=new Label(text);l.getStyleClass().add(style);l.setWrapText(true);
        l.setAlignment(Pos.CENTER); l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);l.setMaxWidth(560);return l;
    }
    public void begin() {
        phase.setText("STARTING YOUR WORKSPACE");status.setText("Preparing your workspace…");detail.setText("Connecting your words, ideas, and possibilities.");
        retry.setVisible(false);retry.setManaged(false);orbit.playFromStart();
    }
    public void setStatus(String value) { status.setText(value); }
    public void showError() {
        stop();phase.setText("STARTUP NEEDS ATTENTION");status.setText("We couldn’t open your workspace.");
        detail.setText("Please try again. If it keeps happening, check the terminal for details.");
        retry.setManaged(true);retry.setVisible(true);retry.requestFocus();
    }
    public void stop() { orbit.stop(); }
    public boolean isAnimating() { return orbit.getStatus()==Animation.Status.RUNNING; }
}
