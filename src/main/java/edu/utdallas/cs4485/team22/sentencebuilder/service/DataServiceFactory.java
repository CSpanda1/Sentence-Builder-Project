package edu.utdallas.cs4485.team22.sentencebuilder.service;

import edu.utdallas.cs4485.team22.sentencebuilder.db.DatabaseConfig;
import edu.utdallas.cs4485.team22.sentencebuilder.db.DatabaseConnection;

/**
 * Chooses a {@link DataService} implementation: JDBC when MySQL is reachable,
 * otherwise the in-memory stub so the UI still runs.
 *
 * @author Nirav Rohra
 * @date 2026-10-16
 */
public final class DataServiceFactory {

    private DataServiceFactory() {
    }

    /**
     * Creates the best available data service for this machine.
     *
     * @return JDBC service or stub
     * @author Nirav Rohra
     * @date 2026-10-16
     */
    public static DataService create() {
        DatabaseConfig config = DatabaseConfig.load();
        if (!config.isPreferJdbc()) {
            System.out.println("[SentenceBuilder] Using StubDataService (db.preferJdbc=false).");
            return new StubDataService();
        }
        DatabaseConnection connection = new DatabaseConnection(config);
        if (connection.canConnect()) {
            System.out.println("[SentenceBuilder] Connected to MySQL at "
                    + config.getHost() + ":" + config.getPort()
                    + "/" + config.getDatabase());
            return new JdbcDataService(connection);
        }
        System.out.println("[SentenceBuilder] MySQL unavailable at "
                + config.jdbcUrl() + " — falling back to StubDataService.");
        System.out.println("[SentenceBuilder] Start DB with: docker compose up -d");
        return new StubDataService();
    }
}
