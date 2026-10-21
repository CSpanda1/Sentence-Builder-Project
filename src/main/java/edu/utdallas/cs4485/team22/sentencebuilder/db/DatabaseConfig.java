package edu.utdallas.cs4485.team22.sentencebuilder.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads database connection settings from {@code application.properties}
 * and optional environment variables / {@code .env} file.
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public final class DatabaseConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final boolean preferJdbc;

    private DatabaseConfig(String host, int port, String database,
                           String user, String password, boolean preferJdbc) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.preferJdbc = preferJdbc;
    }

    /**
     * Loads configuration from the classpath properties file, then applies
     * overrides from a project-root {@code .env} file and environment variables.
     *
     * @return resolved configuration
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public static DatabaseConfig load() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Defaults below still apply.
        }

        loadDotEnv(props);

        String host = firstNonBlank(
                System.getenv("DB_HOST"),
                props.getProperty("db.host"),
                "localhost");
        int port = parsePort(firstNonBlank(
                System.getenv("DB_PORT"),
                props.getProperty("db.port"),
                "3306"));
        String database = firstNonBlank(
                System.getenv("DB_NAME"),
                props.getProperty("db.name"),
                "sentence_builder");
        String user = firstNonBlank(
                System.getenv("DB_USER"),
                props.getProperty("db.user"),
                "sentence");
        String password = firstNonBlank(
                System.getenv("DB_PASS"),
                System.getenv("DB_PASSWORD"),
                props.getProperty("db.password"),
                "sentence");
        boolean preferJdbc = Boolean.parseBoolean(firstNonBlank(
                System.getenv("DB_PREFER_JDBC"),
                props.getProperty("db.preferJdbc"),
                "true"));

        return new DatabaseConfig(host, port, database, user, password, preferJdbc);
    }

    /**
     * @return JDBC URL for MySQL Connector/J
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public String jdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isPreferJdbc() {
        return preferJdbc;
    }

    private static void loadDotEnv(Properties props) {
        Path env = Path.of(".env");
        if (!Files.isRegularFile(env)) {
            env = Path.of("sentence-builder", ".env");
        }
        if (!Files.isRegularFile(env)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(env)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                switch (key) {
                    case "DB_HOST" -> props.setProperty("db.host", value);
                    case "DB_PORT" -> props.setProperty("db.port", value);
                    case "DB_NAME" -> props.setProperty("db.name", value);
                    case "DB_USER" -> props.setProperty("db.user", value);
                    case "DB_PASS", "DB_PASSWORD" -> props.setProperty("db.password", value);
                    case "DB_PREFER_JDBC" -> props.setProperty("db.preferJdbc", value);
                    default -> {
                    }
                }
            }
        } catch (IOException ignored) {
            // Optional file.
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static int parsePort(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 3306;
        }
    }
}
