package dev.districtlife.citizens;

import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.model.Citizen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CitizenDAOTest {

    private TestDatabase db;
    private CitizenDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        db = new TestDatabase();
        Connection conn = db.setup();
        dao = new CitizenDAO(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.teardown();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Citizen makeCitizen(UUID uuid, String firstName, String lastName) {
        return new Citizen(
            uuid.toString(), "MCPlayer",
            firstName, lastName,
            "1990-06-15", System.currentTimeMillis()
        );
    }

    // ── Insert + findByUuid ───────────────────────────────────────────────────

    @Test
    void insert_and_findByUuid_happyPath() throws SQLException {
        UUID uuid = UUID.randomUUID();
        Citizen c = makeCitizen(uuid, "Jean", "Dupont");
        dao.insert(c);

        Optional<Citizen> found = dao.findByUuid(uuid);
        assertTrue(found.isPresent());
        Citizen r = found.get();
        assertEquals("Jean", r.getFirstName());
        assertEquals("Dupont", r.getLastName());
        assertEquals("1990-06-15", r.getBirthDate());
        assertEquals("MCPlayer", r.getMinecraftName());
        assertEquals(uuid.toString(), r.getUuid());
    }

    @Test
    void findByUuid_notFound_returnsEmpty() throws SQLException {
        assertTrue(dao.findByUuid(UUID.randomUUID()).isEmpty());
    }

    @Test
    void insert_duplicateUuid_throwsSQLException() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Jean", "Dupont"));
        assertThrows(SQLException.class,
            () -> dao.insert(makeCitizen(uuid, "Paul", "Martin")));
    }

    @Test
    void insert_duplicateName_throwsSQLException() throws SQLException {
        // L'index UNIQUE sur LOWER(first_name), LOWER(last_name) doit rejeter le doublon
        dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont"));
        assertThrows(SQLException.class,
            () -> dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont")));
    }

    @Test
    void insert_duplicateName_caseInsensitive_throwsSQLException() throws SQLException {
        dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont"));
        assertThrows(SQLException.class,
            () -> dao.insert(makeCitizen(UUID.randomUUID(), "JEAN", "DUPONT")));
    }

    // ── existsByFullName ──────────────────────────────────────────────────────

    @Test
    void existsByFullName_trueWhenExists() throws SQLException {
        dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont"));
        assertTrue(dao.existsByFullName("Jean", "Dupont"));
    }

    @Test
    void existsByFullName_falseWhenAbsent() throws SQLException {
        assertFalse(dao.existsByFullName("Jean", "Dupont"));
    }

    @Test
    void existsByFullName_caseInsensitive() throws SQLException {
        dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont"));
        assertTrue(dao.existsByFullName("jean", "dupont"));
        assertTrue(dao.existsByFullName("JEAN", "DUPONT"));
        assertTrue(dao.existsByFullName("Jean", "dupont"));
    }

    @Test
    void existsByFullName_partialMatchReturnsFalse() throws SQLException {
        dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont"));
        assertFalse(dao.existsByFullName("Jean", "Martin"));
        assertFalse(dao.existsByFullName("Pierre", "Dupont"));
    }

    // ── findByFullName ────────────────────────────────────────────────────────

    @Test
    void findByFullName_returnsCorrectCitizen() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Marie", "Curie"));
        Optional<Citizen> found = dao.findByFullName("Marie", "Curie");
        assertTrue(found.isPresent());
        assertEquals(uuid.toString(), found.get().getUuid());
    }

    @Test
    void findByFullName_caseInsensitive() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Marie", "Curie"));
        assertTrue(dao.findByFullName("MARIE", "CURIE").isPresent());
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    @Test
    void updateFirstName_persists() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Jean", "Dupont"));
        dao.updateFirstName(uuid, "Pierre");
        assertEquals("Pierre", dao.findByUuid(uuid).orElseThrow().getFirstName());
    }

    @Test
    void updateLastName_persists() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Jean", "Dupont"));
        dao.updateLastName(uuid, "Martin");
        assertEquals("Martin", dao.findByUuid(uuid).orElseThrow().getLastName());
    }

    @Test
    void updateBirthDate_persists() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Jean", "Dupont"));
        dao.updateBirthDate(uuid, "2000-01-01");
        assertEquals("2000-01-01", dao.findByUuid(uuid).orElseThrow().getBirthDate());
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteByUuid_removesRecord() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Jean", "Dupont"));
        dao.deleteByUuid(uuid);
        assertTrue(dao.findByUuid(uuid).isEmpty());
    }

    @Test
    void deleteByUuid_nonExistent_noError() throws SQLException {
        // Ne doit pas lever d'exception sur un UUID absent
        assertDoesNotThrow(() -> dao.deleteByUuid(UUID.randomUUID()));
    }

    // ── countAll ──────────────────────────────────────────────────────────────

    @Test
    void countAll_emptyTable_returnsZero() throws SQLException {
        assertEquals(0, dao.countAll());
    }

    @Test
    void countAll_afterInserts_returnsCorrectCount() throws SQLException {
        dao.insert(makeCitizen(UUID.randomUUID(), "Jean", "Dupont"));
        assertEquals(1, dao.countAll());
        dao.insert(makeCitizen(UUID.randomUUID(), "Marie", "Curie"));
        assertEquals(2, dao.countAll());
        dao.insert(makeCitizen(UUID.randomUUID(), "Pierre", "Martin"));
        assertEquals(3, dao.countAll());
    }

    @Test
    void countAll_afterDelete_decrements() throws SQLException {
        UUID uuid = UUID.randomUUID();
        dao.insert(makeCitizen(uuid, "Jean", "Dupont"));
        assertEquals(1, dao.countAll());
        dao.deleteByUuid(uuid);
        assertEquals(0, dao.countAll());
    }
}
