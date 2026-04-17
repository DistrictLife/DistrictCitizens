package dev.districtlife.citizens.database;

import dev.districtlife.citizens.DLCitizensPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final DLCitizensPlugin plugin;
    private Connection connection;

    public DatabaseManager(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), "citizens.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);
            // Les clés étrangères (CASCADE) sont désactivées par défaut dans SQLite JDBC.
            try (java.sql.Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
            new SchemaManager(connection).applyMigrations(plugin);
            plugin.getLogger().info("Base de données SQLite initialisée.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Driver SQLite introuvable : " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur d'initialisation SQLite : " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Erreur à la fermeture de SQLite : " + e.getMessage());
            }
        }
    }
}
