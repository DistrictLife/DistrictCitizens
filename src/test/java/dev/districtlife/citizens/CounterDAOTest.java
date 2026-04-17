package dev.districtlife.citizens;

import dev.districtlife.citizens.database.dao.CounterDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class CounterDAOTest {

    private TestDatabase db;
    private CounterDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        db = new TestDatabase();
        Connection conn = db.setup();
        dao = new CounterDAO(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.teardown();
    }

    // ── Format du serial ──────────────────────────────────────────────────────

    @Test
    void nextSerial_firstCall_returnsCorrectFormat() throws SQLException {
        assertEquals("DL-2026-00001", dao.nextSerial(2026));
    }

    @Test
    void nextSerial_secondCall_increments() throws SQLException {
        dao.nextSerial(2026);
        assertEquals("DL-2026-00002", dao.nextSerial(2026));
    }

    @Test
    void nextSerial_tenCalls_paddedCorrectly() throws SQLException {
        for (int i = 0; i < 9; i++) dao.nextSerial(2026);
        assertEquals("DL-2026-00010", dao.nextSerial(2026));
    }

    @Test
    void nextSerial_zeroPadding_fiveDigits() throws SQLException {
        // Vérifier que le format reste 5 chiffres pour les petits nombres
        String serial = dao.nextSerial(2026);
        assertTrue(serial.matches("DL-2026-\\d{5}"),
            "Le serial doit correspondre à DL-2026-NNNNN, obtenu : " + serial);
    }

    // ── Indépendance des années ───────────────────────────────────────────────

    @Test
    void nextSerial_differentYears_independentCounters() throws SQLException {
        assertEquals("DL-2026-00001", dao.nextSerial(2026));
        assertEquals("DL-2025-00001", dao.nextSerial(2025));
        assertEquals("DL-2026-00002", dao.nextSerial(2026));
        assertEquals("DL-2025-00002", dao.nextSerial(2025));
        assertEquals("DL-2027-00001", dao.nextSerial(2027));
    }

    @Test
    void nextSerial_year2025_correctPrefix() throws SQLException {
        assertEquals("DL-2025-00001", dao.nextSerial(2025));
    }

    // ── Idempotence de l'init ─────────────────────────────────────────────────

    @Test
    void nextSerial_counterInitialisedOnFirstCall() throws SQLException {
        // La ligne dans id_card_counters ne doit pas exister avant le premier appel
        // Le DAO doit la créer et partir de 1
        assertEquals("DL-2026-00001", dao.nextSerial(2026));
    }

    // ── nextSerialInTransaction ───────────────────────────────────────────────

    @Test
    void nextSerialInTransaction_withinExplicitTransaction() throws SQLException {
        Connection conn = db.getConnection();
        conn.setAutoCommit(false);
        try {
            String serial = dao.nextSerialInTransaction(2026);
            assertEquals("DL-2026-00001", serial);
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
        // Le compteur doit avoir été persisté
        assertEquals("DL-2026-00002", dao.nextSerial(2026));
    }

    @Test
    void nextSerialInTransaction_rollback_doesNotPersist() throws SQLException {
        Connection conn = db.getConnection();
        conn.setAutoCommit(false);
        try {
            dao.nextSerialInTransaction(2026); // incrémente à 1
            conn.rollback();                    // annule
        } finally {
            conn.setAutoCommit(true);
        }
        // Après rollback, le compteur repart de 1 (comme si rien ne s'était passé)
        assertEquals("DL-2026-00001", dao.nextSerial(2026));
    }

    // ── Séquentialité garantie ────────────────────────────────────────────────

    @Test
    void nextSerial_sequential_noGaps() throws SQLException {
        for (int i = 1; i <= 5; i++) {
            String expected = String.format("DL-2026-%05d", i);
            assertEquals(expected, dao.nextSerial(2026));
        }
    }
}
