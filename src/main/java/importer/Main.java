package importer;

import javafx.application.Application;

/**
 * Launcher class to start JavaFX Application without requiring VM arguments or module-path configuration.
 *
 * NOTE: Always run this class (Main.java) instead of BookImporter.java in IntelliJ IDEA.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(BookImporter.class, args);
    }
}
