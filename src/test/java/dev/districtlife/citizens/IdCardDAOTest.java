package dev.districtlife.citizens;

import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.database.dao.IdCardDAO;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.model.IdCard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdCardDAOTest {

    private TestDatabase db;
    private CitizenDAO citizenDao;
    private IdCardDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        db = new TestDatabase();
        Connection conn = db.setup();
        citizenDao = new CitizenDAO(conn);
        dao = new IdCardDAO(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.teardown();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID insertParentCitizen(String firstName, String lastName) throws SQLException {
        UUID uuid = UUID.randomUUID();
        citizenDao.insert(new Citizen(
            uuid.toString(), "MCPlayer", firstName, lastName,
            "1990-01-01", System.currentTimeMillis()
        ));
        return uuid;
    }

    private IdCard makeCard(String serial, UUID ownerUuid) {
        return new IdCard(serial, ownerUuid.toString(), System.currentTimeMillis(), 0);
    }

    // ── Insert + findBySerial ─────────────────────────────────────────────────

    @Test
    void insert_and_findBySerial_happyPath() throws SQLException {
        UUID uuid = insertParentCitizen("Jean", "Dupont");
        dao.insert(makeCard("DL-2026-00001", uuid));

        Optional<IdCard> found = dao.findBySerial("DL-2026-00001");
        assertTrue(found.isPresent());
        assertEquals("DL-2026-00001", found.get().getSerial());
        assertEquals(uuid.toString(), found.get().getOwnerUuid());
        assertEquals(0, found.get().getReissueCount());
    }

    @Test
    void findBySerial_notFound_returnsEmpty() throws SQLException {
        assertTrue(dao.findBySerial("DL-2026-99999").isEmpty());
    }

    @Test
    void insert_duplicateSerial_throwsSQLException() throws SQLException {
        UUID uuid = insertParentCitizen("Jean", "Dupont");
        dao.insert(makeCard("DL-2026-00001", uuid));
        assertThrows(SQLException.class,
            () -> dao.insert(makeCard("DL-2026-00001", uuid)));
    }

    // ── findByOwnerUuid ───────────────────────────────────────────────────────

    @Test
    void findByOwnerUuid_happyPath() throws SQLException {
        UUID uuid = insertParentCitizen("Marie", "Curie");
        dao.insert(makeCard("DL-2026-00042", uuid));

        Optional<IdCard> found = dao.findByOwnerUuid(uuid);
        assertTrue(found.isPresent());
        assertEquals("DL-2026-00042", found.get().getSerial());
    }

    @Test
    void findByOwnerUuid_notFound_returnsEmpty() throws SQLException {
        assertTrue(dao.findByOwnerUuid(UUID.randomUUID()).isEmpty());
    }

    // ── incrementReissueCount ─────────────────────────────────────────────────

    @Test
    void incrementReissueCount_startsAtZero_incrementsToOne() throws SQLException {
        UUID uuid = insertParentCitizen("Jean", "Dupont");
        dao.insert(makeCard("DL-2026-00001", uuid));

        dao.incrementReissueCount("DL-2026-00001");
        assertEquals(1, dao.findBySerial("DL-2026-00001").orElseThrow().getReissueCount());
    }

    @Test
    void incrementReissueCount_multipleIncrements() throws SQLException {
        UUID uuid = insertParentCitizen("Jean", "Dupont");
        dao.insert(makeCard("DL-2026-00001", uuid));

        dao.incrementReissueCount("DL-2026-00001");
        dao.incrementReissueCount("DL-2026-00001");
        dao.incrementReissueCount("DL-2026-00001");
        assertEquals(3, dao.findBySerial("DL-2026-00001").orElseThrow().getReissueCount());
    }

    @Test
    void incrementReissueCount_doesNotAffectOtherCards() throws SQLException {
        UUID uuid1 = insertParentCitizen("Jean", "Dupont");
        UUID uuid2 = insertParentCitizen("Marie", "Curie");
        dao.insert(makeCard("DL-2026-00001", uuid1));
        dao.insert(makeCard("DL-2026-00002", uuid2));

        dao.incrementReissueCount("DL-2026-00001");

        assertEquals(1, dao.findBySerial("DL-2026-00001").orElseThrow().getReissueCount());
        assertEquals(0, dao.findBySerial("DL-2026-00002").orElseThrow().getReissueCount());
    }

    // ── Foreign key + CASCADE DELETE ─────────────────────────────────────────

    @Test
    void insert_withoutParentCitizen_throwsForeignKeyViolation() {
        UUID orphan = UUID.randomUUID();
        assertThrows(SQLException.class,
            () -> dao.insert(makeCard("DL-2026-00001", orphan)));
    }

    @Test
    void delete_parentCitizen_cascadesToIdCard() throws SQLException {
        UUID uuid = insertParentCitizen("Jean", "Dupont");
        dao.insert(makeCard("DL-2026-00001", uuid));

        citizenDao.deleteByUuid(uuid);

        assertTrue(dao.findBySerial("DL-2026-00001").isEmpty());
    }
}
