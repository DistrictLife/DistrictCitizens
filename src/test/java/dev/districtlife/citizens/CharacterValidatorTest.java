package dev.districtlife.citizens;

import dev.districtlife.citizens.config.PluginConfig;
import dev.districtlife.citizens.validation.CharacterValidator;
import dev.districtlife.citizens.validation.CharacterValidator.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link CharacterValidator}.
 * Aucune dépendance Bukkit ou SQLite — purement logique métier.
 */
class CharacterValidatorTest {

    // ── Prénom / Nom ──────────────────────────────────────────────────────────

    @Test
    void firstName_valid_simple() {
        assertTrue(CharacterValidator.validateFirstName("Jean").valid());
    }

    @Test
    void firstName_valid_minLength() {
        assertTrue(CharacterValidator.validateFirstName("Lea").valid());
    }

    @Test
    void firstName_valid_maxLength() {
        assertTrue(CharacterValidator.validateFirstName("Jean-BaptisteMar").valid()); // 17 → trop long
        // exact 16
        assertTrue(CharacterValidator.validateFirstName("Jean-BaptisteMa").valid()); // 15 OK
    }

    @Test
    void firstName_valid_hyphen() {
        assertTrue(CharacterValidator.validateFirstName("Jean-Claude").valid());
    }

    @Test
    void firstName_valid_apostrophe() {
        assertTrue(CharacterValidator.validateFirstName("D'Artagnan").valid());
    }

    @Test
    void firstName_valid_twoWordsSeparatedBySpace() {
        assertTrue(CharacterValidator.validateFirstName("Jean Paul").valid());
    }

    @Test
    void firstName_valid_accents() {
        assertTrue(CharacterValidator.validateFirstName("Élodie").valid());
        assertTrue(CharacterValidator.validateFirstName("Noël").valid());
    }

    @Test
    void firstName_invalid_null() {
        ValidationResult r = CharacterValidator.validateFirstName(null);
        assertFalse(r.valid());
        assertEquals("error.name.too_short", r.errorKey());
    }

    @Test
    void firstName_invalid_empty() {
        ValidationResult r = CharacterValidator.validateFirstName("");
        assertFalse(r.valid());
        assertEquals("error.name.too_short", r.errorKey());
    }

    @Test
    void firstName_invalid_tooShort_oneChar() {
        ValidationResult r = CharacterValidator.validateFirstName("Jo");
        assertFalse(r.valid());
        assertEquals("error.name.too_short", r.errorKey());
    }

    @Test
    void firstName_invalid_tooLong() {
        ValidationResult r = CharacterValidator.validateFirstName("A".repeat(17));
        assertFalse(r.valid());
        assertEquals("error.name.too_long", r.errorKey());
    }

    @Test
    void firstName_invalid_digits() {
        ValidationResult r = CharacterValidator.validateFirstName("Jean2");
        assertFalse(r.valid());
        assertEquals("error.name.invalid_chars", r.errorKey());
    }

    @Test
    void firstName_invalid_specialChar() {
        ValidationResult r = CharacterValidator.validateFirstName("Jean!");
        assertFalse(r.valid());
        assertEquals("error.name.invalid_chars", r.errorKey());
    }

    @Test
    void firstName_invalid_doubleSpace() {
        // La regex n'autorise qu'un seul espace interne
        ValidationResult r = CharacterValidator.validateFirstName("Jean  Paul");
        assertFalse(r.valid());
        assertEquals("error.name.invalid_chars", r.errorKey());
    }

    @Test
    void firstName_invalid_leadingSpace() {
        ValidationResult r = CharacterValidator.validateFirstName(" Jean");
        assertFalse(r.valid());
        assertEquals("error.name.invalid_chars", r.errorKey());
    }

    @Test
    void firstName_invalid_trailingSpace() {
        ValidationResult r = CharacterValidator.validateFirstName("Jean ");
        assertFalse(r.valid());
        assertEquals("error.name.invalid_chars", r.errorKey());
    }

    // ── Date de naissance ─────────────────────────────────────────────────────

    private static final int RP_YEAR = 2026;

    @Test
    void birthDate_valid_typical() {
        // 2026 - 1990 = 36 ans → OK
        assertTrue(CharacterValidator.validateBirthDate("1990-06-15", RP_YEAR).valid());
    }

    @Test
    void birthDate_valid_exactlyMinAge() {
        // 2026 - 2008 = 18 → limite basse exacte
        assertTrue(CharacterValidator.validateBirthDate("2008-01-01", RP_YEAR).valid());
    }

    @Test
    void birthDate_valid_exactlyMaxAge() {
        // 2026 - 1936 = 90 → limite haute exacte
        assertTrue(CharacterValidator.validateBirthDate("1936-12-31", RP_YEAR).valid());
    }

    @Test
    void birthDate_invalid_tooYoung() {
        // 2026 - 2012 = 14 → refus
        ValidationResult r = CharacterValidator.validateBirthDate("2012-06-01", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.too_young", r.errorKey());
    }

    @Test
    void birthDate_invalid_justUnderMinAge() {
        // 2026 - 2009 = 17 → refus (17 < 18)
        ValidationResult r = CharacterValidator.validateBirthDate("2009-03-20", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.too_young", r.errorKey());
    }

    @Test
    void birthDate_invalid_tooOld() {
        // 2026 - 1920 = 106 → refus
        ValidationResult r = CharacterValidator.validateBirthDate("1920-01-01", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.too_old", r.errorKey());
    }

    @Test
    void birthDate_invalid_justOverMaxAge() {
        // 2026 - 1935 = 91 → refus (91 > 90)
        ValidationResult r = CharacterValidator.validateBirthDate("1935-07-14", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.too_old", r.errorKey());
    }

    @Test
    void birthDate_invalid_format_slashes() {
        ValidationResult r = CharacterValidator.validateBirthDate("15/06/1990", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.invalid", r.errorKey());
    }

    @Test
    void birthDate_invalid_format_text() {
        ValidationResult r = CharacterValidator.validateBirthDate("not-a-date", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.invalid", r.errorKey());
    }

    @Test
    void birthDate_invalid_impossible_date() {
        // 30 février n'existe pas
        ValidationResult r = CharacterValidator.validateBirthDate("1990-02-30", RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.invalid", r.errorKey());
    }

    @Test
    void birthDate_invalid_null() {
        ValidationResult r = CharacterValidator.validateBirthDate(null, RP_YEAR);
        assertFalse(r.valid());
        assertEquals("error.birthdate.invalid", r.errorKey());
    }

    // ── Apparence ─────────────────────────────────────────────────────────────

    private static PluginConfig cfg() {
        // skinTone max=6, eyeColor max=5, hairStyle max=8, hairColor max=6
        return new PluginConfig(RP_YEAR, 6, 5, 8, 6);
    }

    @Test
    void appearance_valid_allMin() {
        assertTrue(CharacterValidator.validateAppearance(1, 1, 1, 1, cfg()).valid());
    }

    @Test
    void appearance_valid_allMax() {
        assertTrue(CharacterValidator.validateAppearance(6, 5, 8, 6, cfg()).valid());
    }

    @Test
    void appearance_invalid_skinTone_zero() {
        ValidationResult r = CharacterValidator.validateAppearance(0, 1, 1, 1, cfg());
        assertFalse(r.valid());
        assertEquals("error.appearance.out_of_range", r.errorKey());
    }

    @Test
    void appearance_invalid_skinTone_overMax() {
        ValidationResult r = CharacterValidator.validateAppearance(7, 1, 1, 1, cfg());
        assertFalse(r.valid());
        assertEquals("error.appearance.out_of_range", r.errorKey());
    }

    @Test
    void appearance_invalid_eyeColor_zero() {
        assertFalse(CharacterValidator.validateAppearance(1, 0, 1, 1, cfg()).valid());
    }

    @Test
    void appearance_invalid_eyeColor_overMax() {
        assertFalse(CharacterValidator.validateAppearance(1, 6, 1, 1, cfg()).valid());
    }

    @Test
    void appearance_invalid_hairStyle_zero() {
        assertFalse(CharacterValidator.validateAppearance(1, 1, 0, 1, cfg()).valid());
    }

    @Test
    void appearance_invalid_hairStyle_overMax() {
        assertFalse(CharacterValidator.validateAppearance(1, 1, 9, 1, cfg()).valid());
    }

    @Test
    void appearance_invalid_hairColor_zero() {
        assertFalse(CharacterValidator.validateAppearance(1, 1, 1, 0, cfg()).valid());
    }

    @Test
    void appearance_invalid_hairColor_overMax() {
        assertFalse(CharacterValidator.validateAppearance(1, 1, 1, 7, cfg()).valid());
    }

    // ── capitalizeWords ───────────────────────────────────────────────────────

    @Test
    void capitalize_simple() {
        assertEquals("Jean", CharacterValidator.capitalizeWords("jean"));
    }

    @Test
    void capitalize_alreadyCorrect() {
        assertEquals("Jean", CharacterValidator.capitalizeWords("Jean"));
    }

    @Test
    void capitalize_allUpper() {
        assertEquals("Jean", CharacterValidator.capitalizeWords("JEAN"));
    }

    @Test
    void capitalize_twoWords() {
        assertEquals("Jean Paul", CharacterValidator.capitalizeWords("jean paul"));
    }

    @Test
    void capitalize_mixedCase_twoWords() {
        assertEquals("Jean Claude", CharacterValidator.capitalizeWords("JEAN CLAUDE"));
    }

    @Test
    void capitalize_hyphenPreserved() {
        // Le trait d'union n'est pas un séparateur de mots dans capitalizeWords
        assertEquals("Jean-claude", CharacterValidator.capitalizeWords("jean-claude"));
    }

    @Test
    void capitalize_null_returnsNull() {
        assertNull(CharacterValidator.capitalizeWords(null));
    }

    @Test
    void capitalize_empty_returnsEmpty() {
        assertEquals("", CharacterValidator.capitalizeWords(""));
    }
}
