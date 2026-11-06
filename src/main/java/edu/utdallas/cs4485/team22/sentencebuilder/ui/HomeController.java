package edu.utdallas.cs4485.team22.sentencebuilder.ui;

import edu.utdallas.cs4485.team22.sentencebuilder.model.Word;
import edu.utdallas.cs4485.team22.sentencebuilder.service.DataService;
import edu.utdallas.cs4485.team22.sentencebuilder.service.JdbcDataService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.Comparator;
import java.util.List;

/** Welcome workspace. All insight values come from the active data service. */
public final class HomeController implements MainController.Refreshable {
    private final DataService service;
    private final Runnable imports, generate, compose, words, reports;
    private final Label total = text("—", "metric-value"), unique = text("—", "metric-value"),
            sources = text("—", "metric-value"), sentences = text("—", "metric-value");
    private final Label latest = text("Generate a sentence to begin your story.", "preview-sentence");
    private final Label state = text("Loading your workspace…", "guide-copy");
    private final VBox rankings = new VBox(12);
    private long revision;
    public HomeController(DataService service, Runnable imports, Runnable generate,
                          Runnable compose, Runnable words, Runnable reports) {
        this.service=service; this.imports=imports; this.generate=generate;
        this.compose=compose; this.words=words; this.reports=reports;
    }
    private static Label text(String value, String style) {
        Label l = new Label(value); l.getStyleClass().add(style); l.setWrapText(true); l.setMinHeight(Region.USE_PREF_SIZE); return l;
    }
    private static VBox box(String style, Node... nodes) {
        VBox v=new VBox(12, nodes); v.getStyleClass().add(style); return v;
    }
    private static Button action(String title, Runnable run, String style) {
        Button b=new Button(title); b.getStyleClass().add(style); b.setOnAction(e->run.run()); return b;
    }
    private Button entry(String number, String title, String description, String end, Runnable run) {
        VBox content=box("action-content", text(number,"action-number"), text(title,"action-title"),
                text(description,"action-description"),text(end,"action-link"));
        Button b=new Button(); b.setGraphic(content); b.setMaxWidth(Double.MAX_VALUE);
        b.setAccessibleText(title+". "+description); b.setOnAction(e->run.run());
        b.getStyleClass().add("entry-card"); HBox.setHgrow(b,Priority.ALWAYS);
        b.setPrefWidth(300); b.setMinWidth(0); b.prefHeightProperty().bind(javafx.beans.binding.Bindings.when(b.widthProperty().lessThan(265)).then(202).otherwise(164)); b.minHeightProperty().bind(b.prefHeightProperty()); b.maxHeightProperty().bind(b.prefHeightProperty()); content.setMinWidth(0); content.prefWidthProperty().bind(b.widthProperty().subtract(42)); return b;
    }
    public VBox build() {
        VBox root=new VBox(20); root.getStyleClass().add("screen"); root.setPadding(new Insets(24,28,28,28)); root.setMinHeight(Region.USE_PREF_SIZE);
        VBox heading=box("welcome-copy",text("UT DALLAS  /  SENTENCE BUILDER","hero-kicker"),
                text("Small words.\nBig possibilities.","welcome-title"),
                text("Discover how language connects. Build a corpus, follow its patterns, and turn probability into your next sentence.","hero-subtitle"));
        heading.setMaxWidth(620); heading.setMinHeight(Region.USE_PREF_SIZE); HBox.setHgrow(heading,Priority.ALWAYS);
        HBox buttons=new HBox(10,action("Start creating  →",generate,"primary-button"),action("Import a text",imports,"hero-secondary"));
        heading.getChildren().add(buttons);
        ImageView logo=new ImageView(new Image(getClass().getResource("/images/utd-logo.png").toExternalForm()));
        logo.setFitWidth(162); logo.setFitHeight(162); logo.setPreserveRatio(true); logo.setAccessibleText("The University of Texas at Dallas logo");
        VBox emblem=box("emblem-panel",logo,text("BUILT AT UT DALLAS","emblem-caption"));emblem.setAlignment(Pos.CENTER);emblem.setMinWidth(180);
        HBox hero=new HBox(25,heading,new Region(),emblem);HBox.setHgrow(hero.getChildren().get(1),Priority.ALWAYS);
        hero.setAlignment(Pos.CENTER_LEFT); hero.getStyleClass().add("welcome-hero"); root.getChildren().add(hero);
        HBox metrics=new HBox(12);
        String[] labels={"WORDS IN YOUR CORPUS","DISTINCT WORDS","IMPORTED SOURCES","GENERATED SENTENCES"};
        Label[] values={total,unique,sources,sentences};
        for(int i=0;i<4;i++){VBox v=box("metric-card",text(labels[i],"metric-label"),values[i]);v.setPrefWidth(220);HBox.setHgrow(v,Priority.ALWAYS);metrics.getChildren().add(v);}
        root.getChildren().add(metrics);
        HBox section=new HBox(12,text("Where will your words take you?","section-title"),new Region(),text("THREE WAYS TO EXPLORE","section-eyebrow"));
        HBox.setHgrow(section.getChildren().get(1),Priority.ALWAYS);section.setAlignment(Pos.CENTER_LEFT);root.getChildren().add(section);
        HBox entries=new HBox(14,
                entry("01  /  BUILD","Bring your own words","Teach the app with a book, article, or the included sample text.","Import corpus  →",imports),
                entry("02  /  DISCOVER","Let patterns surprise you","Explore three algorithms and compare the sentences they create.","Open generator  →",generate),
                entry("03  /  WRITE","Find your next word","Compose with suggestions based on the words in your corpus.","Start composing  →",compose));
        root.getChildren().add(entries);
        VBox vocabulary=box("surface-card",text("The words shaping your corpus","section-title"),text("Most frequent words • length shows relative frequency","guide-copy"),rankings);
        vocabulary.getChildren().add(action("Explore word library  →",words,"text-button"));vocabulary.setPrefWidth(500);HBox.setHgrow(vocabulary,Priority.ALWAYS);
        VBox story=box("story-card",text("FROM YOUR GENERATOR","section-eyebrow"),latest,state,action("See sentence history  →",reports,"text-button"));
        story.setPrefWidth(340);story.setMinWidth(280);HBox.setHgrow(story,Priority.ALWAYS);
        HBox insight=new HBox(18,vocabulary,story);root.getChildren().add(insight);
        return root;
    }
    private record Ranked(String word,int frequency) {}
    private record Summary(int total,int unique,int files,int sentences,List<Ranked> ranked,String latest) {}
    @Override public void refresh() {
        final long ticket=++revision;
        Task<Summary> task=new Task<>() {
            @Override protected Summary call() {
                var all=service.listWords(null);
                var ranked=all.stream().sorted(Comparator.comparingInt(Word::getFrequency).reversed().thenComparing(Word::getText))
                        .limit(5).map(w->new Ranked(w.getText(),w.getFrequency())).toList();
                var history=service.listSentences();
                return new Summary(service.getTotalWordCount(),all.size(),service.listImportedFiles().size(),history.size(),ranked,
                        history.isEmpty()?null:history.getFirst().getText());
            }
        };
        task.setOnSucceeded(e->{
            if(ticket!=revision)return; Summary s=task.getValue();
            total.setText(String.format("%,d",s.total));unique.setText(String.format("%,d",s.unique));
            sources.setText(String.format("%,d",s.files));sentences.setText(String.format("%,d",s.sentences));
            latest.setText(s.latest==null?"Your next sentence starts with a single word.":"“"+s.latest+"”");
            state.setText(service instanceof JdbcDataService?"Connected to MySQL. Your corpus is stored in the database.":"Demo workspace • seeded vocabulary is ready to explore. Changes last until you close the app.");
            rankings.getChildren().clear();
            int max=s.ranked.stream().mapToInt(Ranked::frequency).max().orElse(1);
            for(Ranked r:s.ranked) {
                Label word=text(r.word,"rank-word");word.setMinWidth(75);word.setPrefWidth(95);word.setTextOverrun(OverrunStyle.ELLIPSIS);word.setWrapText(false);word.setTooltip(new Tooltip(r.word));
                ProgressBar bar=new ProgressBar(r.frequency/(double)Math.max(1,max));bar.setMaxWidth(Double.MAX_VALUE);bar.getStyleClass().add("frequency-bar");HBox.setHgrow(bar,Priority.ALWAYS);
                Label count=text(String.format("%,d",r.frequency),"rank-count");count.setMinWidth(45);count.setAlignment(Pos.CENTER_RIGHT);
                HBox row=new HBox(10,word,bar,count);row.setAlignment(Pos.CENTER_LEFT);rankings.getChildren().add(row);
            }
            if(s.ranked.isEmpty())rankings.getChildren().add(text("No words yet. Import your first text to reveal its patterns.","guide-copy"));
        });
        task.setOnFailed(e->{if(ticket==revision)state.setText("Workspace insights could not load. Check your database connection, then reopen Overview to retry.");});
        Thread worker=new Thread(task,"workspace-insights");worker.setDaemon(true);worker.start();
    }
}
