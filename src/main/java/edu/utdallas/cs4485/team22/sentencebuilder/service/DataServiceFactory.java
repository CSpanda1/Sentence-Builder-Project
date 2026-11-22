package edu.utdallas.cs4485.team22.sentencebuilder.service;

/*****************************************************************************************************************
 * DataServiceFactory.java
 *
 * Creates the application's data service, using JDBC when the configured
 * MySQL database is reachable and an in-memory implementation otherwise.
 *
 * AI Assistance: ChatGPT Codex
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 ******************************************************************************************************************/

import edu.utdallas.cs4485.team22.sentencebuilder.db.DatabaseConfig;
import edu.utdallas.cs4485.team22.sentencebuilder.db.DatabaseConnection;

/**
 * Chooses a {@link DataService} implementation: JDBC when MySQL is reachable,
 * otherwise the in-memory stub so the UI still runs.
 *
 * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026.
 */
public final class DataServiceFactory {

    /**
     * Creates the best available data service for this machine.
     *
     * @return JDBC service or stub
     *
     * Written by: Nirav Rohra(NXR220063) for CS 4485 at the University of Texas at Dallas starting October 16th, 2026
     *
     */
    public static DataService create() {
        // Load database settings from the configured sources.
        DatabaseConfig config = DatabaseConfig.load();
        // Use the in-memory service immediately when JDBC is disabled.
        if (!config.isPreferJdbc()) {
            // Report which data-service implementation was selected.
            System.out.println("[SentenceBuilder] Using StubDataService (db.preferJdbc=false).");
            // Return the in-memory implementation without attempting a database connection.
            return new StubDataService();
        }
        // Create a helper that can test and open connections using this configuration.
        DatabaseConnection connection = new DatabaseConnection(config);
        // Use the JDBC implementation when the MySQL database accepts a connection.
        if (connection.canConnect()) {
            // Report the database location that was reached.
            System.out.println("[SentenceBuilder] Connected to MySQL at "
                    + config.getHost() + ":" + config.getPort()
                    + "/" + config.getDatabase());
            // Return the data service backed by MySQL.
            return new JdbcDataService(connection);
        }
        // Report the failed connection and that the application will use the fallback.
        System.out.println("[SentenceBuilder] MySQL unavailable at "
                + config.jdbcUrl() + " — falling back to StubDataService.");
        // Suggest the project command for starting the database.
        System.out.println("[SentenceBuilder] Start DB with: docker compose up -d");
        // Keep the application usable with the in-memory implementation.
        return new StubDataService();
    }
}
