package dev.districtlife.citizens;

import dev.districtlife.citizens.database.dao.AppearanceDAO;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.database.dao.IdCardDAO;
import dev.districtlife.citizens.model.Appearance;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.transaction.CharacterCreationTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration de {@link CharacterCreationTransaction}.
 * Vérifie l'atomicité, le rollback et la protection contre les race conditions.
 */
class CharacterCreationTransactionTest {

    private static final int RP_YEAR = 2026;

    private TestDatabase db;
    private Connection conn;
    private CharacterCreationTransaction transaction;
    private CitizenDAO citizenDao;
    private AppearanceDAO appearanceDao;
    private IdCardDAO idCardDao;

    @BeforeEach
    void setUp() throws Exception {
        db = new TestDatabase();
        conn = db.setup();
        transaction = new CharacterCreationTransaction(conn);
        citizenDao = new CitizenDAO(conn);
        appearanceDao = new AppearanceDAO(conn);
        idCardDao = new IdCardDAO(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.teardown();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Citizen makeCitizen(String firstName, String lastName) {
        return new Citizen(
            UUID.randomUUID().toString(), "MCPlayer",
            firstName, lastName,
            "1990-01-01", System.currentTimeMillis()
        );
    }

    private static Appearance makeAppearance(String uuid) {
        return new Appearance(uuid, 3, 2, 5, 4);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void createCharacter_happyPath_returnsSerial() throws SQLException {
        Citizen c = makeCitizen("Jean", "Dupont");
        String serial = transaction.createCharacter(c, makeAppearance(c.getUuid()), RP_YEAR);

        assertEquals("DL-2026-00001", serial);
    }

    @Test
    void createCharacter_happyPath_allTablesPopulated() throws SQLException {
        Citizen c = makeCitizen("Jean", "Dupont");
        String serial = transaction.createCharacter(c, makeAppearance(c.getUuid()), RP_YEAR);

        // citizen
        assertTrue(citizenDao.findByUuid(UUID.fromString(c.getUuid())).isPresent());
        // appearance
        assertTrue(appearanceDao.findByUuid(UUID.fromString(c.getUuid())).isPresent());
        // id_card
        assertTrue(idCardDao.findBySerial(serial).isPresent());
        assertEquals(c.getUuid(), idCardDao.findBySerial(serial).orElseThrow().getOwnerUuid());
    }

    @Test
    void createCharacter_serialIncrements_acrossMultipleCalls() throws SQLException {
        Citizen c1 = makeCitizen("Jean", "Dupont");
        Citizen c2 = makeCitizen("Marie", "Curie");
        String s1 = transaction.createCharacter(c1, makeAppearance(c1.getUuid()), RP_YEAR);
        String s2 = transaction.createCharacter(c2, makeAppearance(c2.getUuid()), RP_YEAR);
        assertEquals("DL-2026-00001", s1);
        assertEquals("DL-2026-00002", s2);
    }

    @Test
    void createCharacter_appearsInCountAll() throws SQLException {
        assertEquals(0, citizenDao.countAll());
        Citizen c = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c, makeAppearance(c.getUuid()), RP_YEAR);
        assertEquals(1, citizenDao.countAll());
    }

    // ── Race condition : unicité du nom ───────────────────────────────────────

    @Test
    void createCharacter_duplicateName_throwsRaceConditionException() throws SQLException {
        Citizen c1 = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c1, makeAppearance(c1.getUuid()), RP_YEAR);

        Citizen c2 = makeCitizen("Jean", "Dupont"); // même nom, UUID différent
        SQLException ex = assertThrows(SQLException.class,
            () -> transaction.createCharacter(c2, makeAppearance(c2.getUuid()), RP_YEAR));
        assertEquals("race_condition:name_taken", ex.getMessage());
    }

    @Test
    void createCharacter_duplicateName_caseInsensitive_throwsRaceCondition() throws SQLException {
        Citizen c1 = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c1, makeAppearance(c1.getUuid()), RP_YEAR);

        Citizen c2 = makeCitizen("JEAN", "DUPONT");
        assertThrows(SQLException.class,
            () -> transaction.createCharacter(c2, makeAppearance(c2.getUuid()), RP_YEAR));
    }

    // ── Atomicité : rollback sur erreur ───────────────────────────────────────

    @Test
    void createCharacter_duplicateName_rollback_noPartialState() throws SQLException {
        Citizen c1 = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c1, makeAppearance(c1.getUuid()), RP_YEAR);
        assertEquals(1, citizenDao.countAll());

        Citizen c2 = makeCitizen("Jean", "Dupont");
        try {
            transaction.createCharacter(c2, makeAppearance(c2.getUuid()), RP_YEAR);
        } catch (SQLException ignored) {}

        // Toujours 1 citoyen — aucun état partiel n'a été persisté
        assertEquals(1, citizenDao.countAll());
        assertFalse(appearanceDao.findByUuid(UUID.fromString(c2.getUuid())).isPresent());
    }

    @Test
    void createCharacter_afterFailedTransaction_counterNotAdvanced() throws SQLException {
        Citizen c1 = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c1, makeAppearance(c1.getUuid()), RP_YEAR);

        // Tentative échouée avec le même nom
        Citizen c2 = makeCitizen("Jean", "Dupont");
        try {
            transaction.createCharacter(c2, makeAppearance(c2.getUuid()), RP_YEAR);
        } catch (SQLException ignored) {}

        // Le prochain citoyen valide doit obtenir le serial 00002 (pas 00003)
        Citizen c3 = makeCitizen("Marie", "Curie");
        String serial = transaction.createCharacter(c3, makeAppearance(c3.getUuid()), RP_YEAR);
        assertEquals("DL-2026-00002", serial);
    }

    // ── Isolation autoCommit ──────────────────────────────────────────────────

    @Test
    void createCharacter_restoresAutoCommitAfterSuccess() throws SQLException {
        assertTrue(conn.getAutoCommit(), "autoCommit doit être true avant la transaction");
        Citizen c = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c, makeAppearance(c.getUuid()), RP_YEAR);
        assertTrue(conn.getAutoCommit(), "autoCommit doit être restauré à true après succès");
    }

    @Test
    void createCharacter_restoresAutoCommitAfterFailure() throws SQLException {
        Citizen c1 = makeCitizen("Jean", "Dupont");
        transaction.createCharacter(c1, makeAppearance(c1.getUuid()), RP_YEAR);

        Citizen c2 = makeCitizen("Jean", "Dupont");
        try {
            transaction.createCharacter(c2, makeAppearance(c2.getUuid()), RP_YEAR);
        } catch (SQLException ignored) {}

        assertTrue(conn.getAutoCommit(), "autoCommit doit être restauré à true après rollback");
    }
}
