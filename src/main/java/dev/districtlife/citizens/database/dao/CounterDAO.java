package dev.districtlife.citizens.database.dao;

import java.sql.*;

public class CounterDAO {

    private final Connection connection;

    public CounterDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Génère le prochain serial au format DL-YYYY-NNNNN en gérant sa propre transaction.
     * À utiliser uniquement quand {@code connection} est en autoCommit=true (appel standalone).
     * Depuis {@link dev.districtlife.citizens.transaction.CharacterCreationTransaction},
     * utiliser {@link #nextSerialInTransaction(int)} à la place.
     */
    public String nextSerial(int year) throws SQLException {
        if (!connection.getAutoCommit()) {
            // Déjà dans une transaction externe — déléguer directement.
            return nextSerialInTransaction(year);
        }
        connection.setAutoCommit(false);
        try {
            String serial = nextSerialInTransaction(year);
            connection.commit();
            return serial;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Variante à appeler depuis une transaction déjà ouverte par l'appelant.
     * Ne touche pas à autoCommit, commit ni rollback.
     *
     * Algorithme :
     *   1. INSERT OR IGNORE → crée la ligne si absente (counter = 0)
     *   2. UPDATE counter = counter + 1
     *   3. SELECT counter → formater le serial
     */
    public String nextSerialInTransaction(int year) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO id_card_counters (year, counter) VALUES (?, 0)")) {
            insert.setInt(1, year);
            insert.executeUpdate();
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE id_card_counters SET counter = counter + 1 WHERE year = ?")) {
            update.setInt(1, year);
            update.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT counter FROM id_card_counters WHERE year = ?")) {
            select.setInt(1, year);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Impossible de lire le compteur pour l'année " + year);
                }
                return String.format("DL-%d-%05d", year, rs.getInt(1));
            }
        }
    }
}
