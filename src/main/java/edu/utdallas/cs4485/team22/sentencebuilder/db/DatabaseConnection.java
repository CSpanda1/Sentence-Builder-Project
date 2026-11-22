package edu.utdallas.cs4485.team22.sentencebuilder.db;

/*****************************************************************************************************************
 * DatabaseConnection.java
 *
 * Opens and checks MySQL JDBC connections using settings from DatabaseConfig.
 * Callers are responsible for closing connections returned by open().
 *
 * AI Assistance:ChatGPT Codex. Some methods were generated with ChatGPT Codex
 * Written by: Ahmed Sherwani(AJS170005) and Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting November 20th, 2026.
 ******************************************************************************************************************/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens short-lived JDBC connections to MySQL using {@link DatabaseConfig}.
 *
 * <p>Each call opens and the caller closes the connection (professor's
 * preferred pattern for this course size).</p>
 *
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public final class DatabaseConnection {

    // The database configuration settings used to open connections.
    private final DatabaseConfig config;

    /**
     * @param config database settings
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Opens a new connection to MySQL.
     *
     * @return open connection (caller must close)
     * @throws SQLException if the driver cannot connect
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public Connection open() throws SQLException {
        // Use DriverManager to open a connection using the JDBC URL, username, and password from the config.
        return DriverManager.getConnection(
                config.jdbcUrl(), config.getUser(), config.getPassword());
    }

    /**
     * Probes whether MySQL is reachable with the current settings.
     *
     * @return true if a connection can be opened and closed
     * Written by: Nirav Rohra(NXR220063) and Ahmed Sherwani(AJS170005) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
     */
    public boolean canConnect() {
        // Attempt to open a connection and immediately close it. If successful, return true; otherwise, return false.
        try (Connection ignored = open()) {
            // If the connection opens successfully, return true.
            return true;
        } catch (SQLException e) {
            // If an exception occurs while trying to open the connection, return false.
            return false;
        }
    }
}
