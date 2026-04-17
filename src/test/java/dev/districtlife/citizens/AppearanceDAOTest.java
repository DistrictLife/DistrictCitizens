package dev.districtlife.citizens;

import dev.districtlife.citizens.database.dao.AppearanceDAO;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.model.Appearance;
import dev.districtlife.citizens.model.Citizen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppearanceDAOTest {

    private TestDatabase db;
    private CitizenDAO citizenDao;
    private AppearanceDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        db = new TestDatabase();
        Connection conn = db.setup();
        citizenDao = new CitizenDAO(conn);
        dao = new AppearanceDAO(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.teardown();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Insère un citoyen parent (requis par la FK de appearances). */
    private UUID insertParentCitizen() throws SQLException {
        UUID uuid = UUID.randomUUID();
        citizenDao.insert(new Citizen(
            uuid.toString(), "MCPlayer", "Jean", "Dupont",
            "1990-01-01", System.currentTimeMillis()
        ));
        return uuid;
    }

    // ── Insert + findByUuid ───────────────────────────────────────────────────

    @Test
    void insert_and_findByUuid_happyPath() throws SQLException {
        UUID uuid = insertParentCitizen();
        dao.insert(new Appearance(uuid.toString(), 3, 2, 5, 4));

        Optional<Appearance> found = dao.findByUuid(uuid);
        assertTrue(found.isPresent());
        Appearance a = found.get();
        assertEquals(uuid.toString(), a.getUuid());
        assertEquals(3, a.getSkinTone());
        assertEquals(2, a.getEyeColor());
        assertEquals(5, a.getHairStyle());
        assertEquals(4, a.getHairColor());
    }

    @Test
    void findByUuid_notFound_returnsEmpty() throws SQLException {
        assertTrue(dao.findByUuid(UUID.randomUUID()).isEmpty());
    }

    @Test
    void insert_withoutParentCitizen_throwsForeignKeyViolation() {
        // La FK doit empêcher l'insertion d'une apparence sans citoyen parent
        UUID orphanUuid = UUID.randomUUID();
        assertThrows(SQLException.class,
            () -> dao.insert(new Appearance(orphanUuid.toString(), 1, 1, 1, 1)));
    }

    // ── updateByUuid ─────────────────────────────────────────────────────────

    @Test
    void updateByUuid_allFields_persist() throws SQLException {
        UUID uuid = insertParentCitizen();
        dao.insert(new Appearance(uuid.toString(), 1, 1, 1, 1));

        dao.updateByUuid(new Appearance(uuid.toString(), 6, 5, 8, 6));

        Appearance updated = dao.findByUuid(uuid).orElseThrow();
        assertEquals(6, updated.getSkinTone());
        assertEquals(5, updated.getEyeColor());
        assertEquals(8, updated.getHairStyle());
        assertEquals(6, updated.getHairColor());
    }

    @Test
    void updateByUuid_onlyChangesTargetRecord() throws SQLException {
        UUID uuid1 = insertParentCitizen();
        UUID uuid2 = UUID.randomUUID();
        citizenDao.insert(new Citizen(uuid2.toString(), "P2", "Marie", "Curie",
            "1980-01-01", System.currentTimeMillis()));

        dao.insert(new Appearance(uuid1.toString(), 1, 1, 1, 1));
        dao.insert(new Appearance(uuid2.toString(), 2, 2, 2, 2));

        dao.updateByUuid(new Appearance(uuid1.toString(), 5, 4, 7, 3));

        // uuid2 doit être inchangé
        Appearance a2 = dao.findByUuid(uuid2).orElseThrow();
        assertEquals(2, a2.getSkinTone());
        assertEquals(2, a2.getEyeColor());
    }

    // ── CASCADE DELETE ────────────────────────────────────────────────────────

    @Test
    void delete_parentCitizen_cascadesToAppearance() throws SQLException {
        UUID uuid = insertParentCitizen();
        dao.insert(new Appearance(uuid.toString(), 2, 3, 4, 5));

        // Supprimer le citoyen doit supprimer l'apparence en cascade
        citizenDao.deleteByUuid(uuid);

        assertTrue(dao.findByUuid(uuid).isEmpty());
    }
}
