package edu.utdallas.cs4485.team22.sentencebuilder.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens short-lived JDBC connections to MySQL using {@link DatabaseConfig}.
 *
 * <p>Each call opens and the caller closes the connection (professor's
 * preferred pattern for this course size).</p>
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public final class DatabaseConnection {

    private final DatabaseConfig config;

    /**
     * @param config database settings
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Opens a new connection to MySQL.
     *
     * @return open connection (caller must close)
     * @throws SQLException if the driver cannot connect
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public Connection open() throws SQLException {
        return DriverManager.getConnection(
                config.jdbcUrl(), config.getUser(), config.getPassword());
    }

    /**
     * Probes whether MySQL is reachable with the current settings.
     *
     * @return true if a connection can be opened and closed
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public boolean canConnect() {
        try (Connection ignored = open()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
