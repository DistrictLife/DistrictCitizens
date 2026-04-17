package dev.districtlife.citizens;

import dev.districtlife.citizens.database.SchemaManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Helper de test : crée une base SQLite in-memory fraîche à chaque appel de {@link #setup()}.
 * Pas de dépendance Bukkit ou Minecraft.
 */
public class TestDatabase {

    private static final Logger LOG = Logger.getLogger("DLCitizens-Test");

    private Connection connection;

    /**
     * Ouvre une connexion SQLite en mémoire, active les clés étrangères,
     * et applique le schéma V1.
     */
    public Connection setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        // ":memory:" garantit une base totalement indépendante pour chaque test
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        connection.setAutoCommit(true);

        try (Statement pragma = connection.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON");
        }

        new SchemaManager(connection).applyMigrations(LOG);
        return connection;
    }

    public void teardown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
