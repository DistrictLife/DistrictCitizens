package dev.districtlife.citizens.transaction;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.AppearanceDAO;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.database.dao.CounterDAO;
import dev.districtlife.citizens.database.dao.IdCardDAO;
import dev.districtlife.citizens.model.Appearance;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.model.IdCard;

import java.sql.Connection;
import java.sql.SQLException;

public class CharacterCreationTransaction {

    private final Connection connection;

    /** Constructeur principal — injection directe de la connexion (testable sans Bukkit). */
    public CharacterCreationTransaction(Connection connection) {
        this.connection = connection;
    }

    /** Constructeur de commodité Bukkit. */
    public CharacterCreationTransaction(DLCitizensPlugin plugin) {
        this(plugin.getDatabaseManager().getConnection());
    }

    /**
     * Crée un personnage en une seule transaction SQLite atomique.
     * Retourne le serial généré (ex : "DL-2026-00042").
     *
     * Étapes dans la transaction :
     *   1. Re-vérifie l'unicité prénom+nom (protection contre la race condition)
     *   2. INSERT citizens
     *   3. INSERT appearances
     *   4. Incrémente le compteur et récupère le serial
     *   5. INSERT id_cards
     *
     * Lève {@link SQLException} avec le message "race_condition:name_taken"
     * si le nom est déjà pris au moment de la transaction.
     */
    public String createCharacter(Citizen citizen, Appearance appearance, int rpYear) throws SQLException {
        boolean wasAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            CitizenDAO citizenDao   = new CitizenDAO(connection);
            AppearanceDAO appDao    = new AppearanceDAO(connection);
            IdCardDAO cardDao       = new IdCardDAO(connection);
            CounterDAO counterDao   = new CounterDAO(connection);

            // Étape 1 — re-vérifie l'unicité (race condition)
            if (citizenDao.existsByFullName(citizen.getFirstName(), citizen.getLastName())) {
                throw new SQLException("race_condition:name_taken");
            }

            // Étape 2 — INSERT citizens
            citizenDao.insert(citizen);

            // Étape 3 — INSERT appearances
            appDao.insert(appearance);

            // Étape 4 — serial (sans gestion de transaction propre puisqu'on est déjà dedans)
            String serial = counterDao.nextSerialInTransaction(rpYear);

            // Étape 5 — INSERT id_cards
            IdCard idCard = new IdCard(serial, citizen.getUuid(), System.currentTimeMillis(), 0);
            cardDao.insert(idCard);

            connection.commit();
            return serial;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(wasAutoCommit);
        }
    }
}
