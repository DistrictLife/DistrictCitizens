package dev.districtlife.citizens.database;

import dev.districtlife.citizens.DLCitizensPlugin;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class SchemaManager {

    private final Connection connection;

    public SchemaManager(Connection connection) {
        this.connection = connection;
    }

    /**
     * Point d'entrée Bukkit — délègue vers la méthode découplée.
     */
    public void applyMigrations(DLCitizensPlugin plugin) {
        applyMigrations(plugin.getLogger());
    }

    /**
     * Point d'entrée standalone/test — charge le SQL depuis le classpath.
     * Utilise {@code java.util.logging.Logger} pour ne pas dépendre de Bukkit.
     */
    public void applyMigrations(Logger logger) {
        try {
            ensureSchemaVersionTable();
            int version = getSchemaVersion();

            if (version < 1) {
                try (InputStream is = SchemaManager.class.getClassLoader()
                        .getResourceAsStream("db/migrations/V1__initial_schema.sql")) {
                    if (is == null) {
                        logger.severe("Ressource SQL introuvable : db/migrations/V1__initial_schema.sql");
                        return;
                    }
                    applySql(is, logger);
                }
                setSchemaVersion(1);
                logger.info("Migration V1 appliquée.");
            }
        } catch (Exception e) {
            logger.severe("Erreur lors des migrations : " + e.getMessage());
        }
    }

    private void ensureSchemaVersionTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY)"
            );
        }
    }

    private int getSchemaVersion() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (rs.next()) {
                int v = rs.getInt(1);
                return rs.wasNull() ? 0 : v;
            }
            return 0;
        }
    }

    private void setSchemaVersion(int version) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT OR REPLACE INTO schema_version (version) VALUES (" + version + ")");
        }
    }

    private void applySql(InputStream is, Logger logger) {
        try {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Erreur lors de l'application du SQL : " + e.getMessage());
        }
    }
}
