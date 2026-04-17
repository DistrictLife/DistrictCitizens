package dev.districtlife.citizens.validation;

import dev.districtlife.citizens.config.PluginConfig;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class CharacterValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\-']+(?: [\\p{L}\\-']+)?$");
    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 16;
    private static final int AGE_MIN = 18;
    private static final int AGE_MAX = 90;

    public static final class ValidationResult {
        private final boolean valid;
        private final String errorKey;
        public ValidationResult(boolean valid, String errorKey) {
            this.valid = valid;
            this.errorKey = errorKey;
        }
        public boolean valid() { return valid; }
        public String errorKey() { return errorKey; }
    }

    public static ValidationResult validateFirstName(String value) {
        return validateName(value);
    }

    public static ValidationResult validateLastName(String value) {
        return validateName(value);
    }

    private static ValidationResult validateName(String value) {
        if (value == null || value.length() < NAME_MIN) {
            return new ValidationResult(false, "error.name.too_short");
        }
        if (value.length() > NAME_MAX) {
            return new ValidationResult(false, "error.name.too_long");
        }
        if (!NAME_PATTERN.matcher(value).matches()) {
            return new ValidationResult(false, "error.name.invalid_chars");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateBirthDate(String isoDate, int rpYear) {
        if (isoDate == null) return new ValidationResult(false, "error.birthdate.invalid");

        LocalDate date;
        try {
            date = LocalDate.parse(isoDate);
        } catch (DateTimeParseException e) {
            return new ValidationResult(false, "error.birthdate.invalid");
        }

        int age = rpYear - date.getYear();
        if (age < AGE_MIN) {
            return new ValidationResult(false, "error.birthdate.too_young");
        }
        if (age > AGE_MAX) {
            return new ValidationResult(false, "error.birthdate.too_old");
        }

        return new ValidationResult(true, "");
    }

    public static ValidationResult validateAppearance(int skinTone, int eyeColor,
                                                       int hairStyle, int hairColor,
                                                       PluginConfig cfg) {
        if (skinTone < 1 || skinTone > cfg.getSkinToneCount()) {
            return new ValidationResult(false, "error.appearance.out_of_range");
        }
        if (eyeColor < 1 || eyeColor > cfg.getEyeColorCount()) {
            return new ValidationResult(false, "error.appearance.out_of_range");
        }
        if (hairStyle < 1 || hairStyle > cfg.getHairStyleCount()) {
            return new ValidationResult(false, "error.appearance.out_of_range");
        }
        if (hairColor < 1 || hairColor > cfg.getHairColorCount()) {
            return new ValidationResult(false, "error.appearance.out_of_range");
        }
        return new ValidationResult(true, "");
    }

    /**
     * Capitalise chaque mot : première lettre majuscule, reste minuscules.
     */
    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] parts = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
