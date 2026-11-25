package edu.utdallas.cs4485.team22.sentencebuilder.db;


/*****************************************************************************************************************
 * DatabaseConfig.java
 *
 * Loads MySQL connection settings from application.properties, a project-root
 * .env file, and environment variables, with defaults for missing values.
 * Provides the resolved settings and builds the JDBC connection URL
 *
 * AI Assistance:ChatGPT Codex. Some methods were generated with ChatGPT Codex
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting November 20th, 2026.
 ******************************************************************************************************************/


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads database connection settings from {@code application.properties}
 * and optional environment variables / {@code .env} file.
 *
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public final class DatabaseConfig {

    // Database connection settings for host
    private final String host;
    // Database connection settings for port
    private final int port;
    // Database connection settings for database name
    private final String database;
    // Database connection settings for user
    private final String user;
    // Database connection settings for password
    private final String password;
    // Database connection settings for preferJdbc
    private final boolean preferJdbc;

    /**
     * Private constructor to initialize the database configuration.
     * @param host the database host
     * @param port the database port
     * @param database the database name
     * @param user the database user
     * @param password the database password
     * @param preferJdbc whether to prefer JDBC
     * This method was created with ChatGPT Codex
     * Prompt: "Write a constructor for the DatabaseConfig class that initializes the database configuration fields. The constructor should take parameters for host, port, database, user, password, and preferJdbc. The constructor should be private to enforce the use of the load() method for creating instances of DatabaseConfig."
     * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    private DatabaseConfig(String host, int port, String database,
                           String user, String password, boolean preferJdbc)
    {
        // Initialize the database configuration fields.
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
     * This method was created with ChatGPT Codex
     * Prompt: "Write a static method load() for the DatabaseConfig class that loads configuration from the classpath properties file, then applies overrides from a project-root .env file and environment variables. The method should return an instance of DatabaseConfig with the resolved configuration."
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     *
     */
    public static DatabaseConfig load() {
        // Load properties from application.properties in the classpath.
        Properties props = new Properties();
        // Use try-with-resources to ensure the InputStream is closed after use.
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Defaults below still apply.
        }

        // Load overrides from .env file in the project root.
        loadDotEnv(props);

        // Store host from environment variables from .env
        String host = firstNonBlank(
                System.getenv("DB_HOST"),
                props.getProperty("db.host"),
                "localhost");
        // Store port from environment variables from .env
        int port = parsePort(firstNonBlank(
                System.getenv("DB_PORT"),
                props.getProperty("db.port"),
                "3306"));

        // Store database name from environment variables from .env
        String database = firstNonBlank(
                System.getenv("DB_NAME"),
                props.getProperty("db.name"),
                "sentence_builder");
        // Store user from environment variables from .env
        String user = firstNonBlank(
                System.getenv("DB_USER"),
                props.getProperty("db.user"),
                "sentence");
        // Store password from environment variables from .env
        String password = firstNonBlank(
                System.getenv("DB_PASS"),
                System.getenv("DB_PASSWORD"),
                props.getProperty("db.password"),
                "sentence");
        // Store preferJdbc from environment variables from .env using boolean
        boolean preferJdbc = Boolean.parseBoolean(firstNonBlank(
                System.getenv("DB_PREFER_JDBC"),
                props.getProperty("db.preferJdbc"),
                "true"));

        // Return a new instance of DatabaseConfig with the resolved configuration.
        return new DatabaseConfig(host, port, database, user, password, preferJdbc);
    }

    /**
     * Returns the JDBC URL for MySQL Connector/J.
     * @return JDBC URL for MySQL Connector/J
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String jdbcUrl() {
        // Format the JDBC URL using the host, port, and database name.
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database);
    }

    /**
     * Returns the host of the database.
     * @return the host of the database
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getHost() {
        // Return the host of the database.
        return host;
    }

    /**
     * Returns the port of the database.
     * @return the port of the database
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public int getPort() {
        // Return the port of the database.
        return port;
    }

    /**
     * Returns the name of the database.
     * @return the name of the database
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getDatabase() {
        // Return the name of the database.
        return database;
    }

    /**
     * Returns the user of the database.
     * @return the user of the database
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getUser() {
        // Return the user of the database.
        return user;
    }

    /**
     * Returns the password of the database.
     * @return the password of the database
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public String getPassword() {
        // Return the password of the database.
        return password;
    }

    /**
     * Returns whether to prefer JDBC.
     * @return whether to prefer JDBC
     *
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    public boolean isPreferJdbc() {
        // Return whether to prefer JDBC.
        return preferJdbc;
    }

    /**
     * Loads overrides from a project-root {@code .env} file into the provided
     * {@link Properties} object.
     *
     * @param props the properties object to load overrides into
     * This method was created with ChatGPT Codex
     * Prompt: "Write a private static method loadDotEnv(Properties props) for the DatabaseConfig class that loads overrides from a project-root .env file into the provided Properties object. The method should read the .env file line by line, parse key-value pairs, and set the corresponding properties in the props object."
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    private static void loadDotEnv(Properties props) {
        // Look for a .env file in the current working directory.
        Path env = Path.of(".env");
        // If it is not a regular file, check the project subdirectory.
        if (!Files.isRegularFile(env)) {
            // Use the .env file under sentence-builder instead.
            env = Path.of("sentence-builder", ".env");
        }
        // Stop if neither possible .env location contains a file.
        if (!Files.isRegularFile(env)) {
            // No .env overrides are available, so keep the current properties unchanged.
            return;
        }
        try {
            // Read the file and process each line.
            for (String line : Files.readAllLines(env)) {
                // Remove whitespace around the line.
                String trimmed = line.trim();
                // Ignore blank lines, comments, and lines without a key-value separator.
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    // Skip this line and continue with the next one.
                    continue;
                }
                // Find the first '=' separating the variable name from its value.
                int eq = trimmed.indexOf('=');
                // Extract and trim the variable name.
                String key = trimmed.substring(0, eq).trim();
                // Extract and trim everything after the first '=' as the value.
                String value = trimmed.substring(eq + 1).trim();
                // Map supported environment-style names to application property names.
                switch (key) {
                    // Set the database server address.
                    case "DB_HOST" -> props.setProperty("db.host", value);
                    // Set the database server port.
                    case "DB_PORT" -> props.setProperty("db.port", value);
                    // Set the database name.
                    case "DB_NAME" -> props.setProperty("db.name", value);
                    // Set the database username.
                    case "DB_USER" -> props.setProperty("db.user", value);
                    // Accept either supported password variable name.
                    case "DB_PASS", "DB_PASSWORD" -> props.setProperty("db.password", value);
                    // Set whether the application prefers JDBC.
                    case "DB_PREFER_JDBC" -> props.setProperty("db.preferJdbc", value);
                    default -> {
                        // Ignore keys not supported by this configuration loader.
                    }
                }
            }
        } catch (IOException ignored) {
            // Keep existing settings if the optional .env file cannot be read.
        }
    }

    /**
     * Returns the first non-blank string from the provided values, or an empty
     * string if all values are blank or null.
     *
     * @param values the strings to check
     * @return the first non-blank string, or an empty string if none found
     * This method was created with ChatGPT Codex
     * Prompt: "Write a private static method firstNonBlank(String... values) for the DatabaseConfig class that returns the first non-blank string from the provided values, or an empty string if all values are blank or null."
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    private static String firstNonBlank(String... values) {
        // Iterate through the provided values to find the first non-blank string.
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        // If all values are blank or null, return an empty string.
        return "";
    }

    /**
     * Parses a string as an integer port number, returning a default value of
     * 3306 if the string is not a valid integer.
     *
     * @param raw the string to parse
     * @return the parsed port number, or 3306 if parsing fails
     * This method was created with ChatGPT Codex
     * Prompt: "Write a private static method parsePort(String raw) for the DatabaseConfig class that parses a string as an integer port number, returning a default value of 3306 if the string is not a valid integer."
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16, 2026.
     */
    private static int parsePort(String raw) {
        try {
            // Attempt to parse the string as an integer and return the result.
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            // If parsing fails, return the default port number 3306.
            return 3306;
        }
    }
}
