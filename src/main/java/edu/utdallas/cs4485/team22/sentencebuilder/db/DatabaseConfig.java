package edu.utdallas.cs4485.team22.sentencebuilder.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads database connection settings from {@code application.properties},
 * {@code .env}, and environment variables.
 *
 * <p>Supports Team 22 local settings ({@code DB_*}) and Ahmed's AWS RDS
 * variables ({@code DATABASE_URL}, {@code DATABASE_USERNAME},
 * {@code DATABASE_PASSWORD}).</p>
 *
 * @author Nirav Rohra
 * @author Ahmed Sherwani
 * @date 2026-10-16
 */
public final class DatabaseConfig {

    private final String jdbcUrl;
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final boolean preferJdbc;

    private DatabaseConfig(String jdbcUrl, String host, int port, String database,
                           String user, String password, boolean preferJdbc) {
        this.jdbcUrl = jdbcUrl;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.preferJdbc = preferJdbc;
    }

    /**
     * Loads configuration with env overrides.
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

        String awsUrl = firstNonBlank(System.getenv("DATABASE_URL"), props.getProperty("database.url"));
        String user = firstNonBlank(
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DB_USER"),
                props.getProperty("db.user"),
                "sentence");
        String password = firstNonBlank(
                System.getenv("DATABASE_PASSWORD"),
                System.getenv("DB_PASS"),
                System.getenv("DB_PASSWORD"),
                props.getProperty("db.password"),
                "sentence");
        boolean preferJdbc = Boolean.parseBoolean(firstNonBlank(
                System.getenv("DB_PREFER_JDBC"),
                props.getProperty("db.preferJdbc"),
                "true"));

        if (!awsUrl.isBlank()) {
            return new DatabaseConfig(awsUrl, "(remote)", 3306, "(from DATABASE_URL)",
                    user, password, preferJdbc);
        }

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
        String url = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database);
        return new DatabaseConfig(url, host, port, database, user, password, preferJdbc);
    }

    /**
     * @return JDBC URL (local or AWS RDS)
     * @author Ahmed Sherwani
     * @date 2026-10-17
     */
    public String jdbcUrl() {
        return jdbcUrl;
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
                    case "DB_USER", "DATABASE_USERNAME" -> props.setProperty("db.user", value);
                    case "DB_PASS", "DB_PASSWORD", "DATABASE_PASSWORD" -> props.setProperty("db.password", value);
                    case "DATABASE_URL" -> props.setProperty("database.url", value);
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
