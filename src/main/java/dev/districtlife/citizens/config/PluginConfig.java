package dev.districtlife.citizens.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final int rpYear;
    private final String creationWorld;
    private final double creationX;
    private final double creationY;
    private final double creationZ;
    private final float creationYaw;
    private final int creationTimeoutSeconds;
    private final int skinToneCount;
    private final int eyeColorCount;
    private final int hairStyleCount;
    private final int hairColorCount;
    private final int rateLimitCheckNamePerMinute;

    /** Constructeur principal — production (Bukkit). */
    public PluginConfig(FileConfiguration config) {
        rpYear = config.getInt("rp_year", 2026);

        creationWorld = config.getString("creation_zone.world", "world");
        creationX = config.getDouble("creation_zone.x", 0.5);
        creationY = config.getDouble("creation_zone.y", 64.0);
        creationZ = config.getDouble("creation_zone.z", 0.5);
        creationYaw = (float) config.getDouble("creation_zone.yaw", 0.0);

        creationTimeoutSeconds = config.getInt("creation_protection.timeout_seconds", 300);

        skinToneCount = config.getInt("appearance.skin_tone_count", 6);
        eyeColorCount = config.getInt("appearance.eye_color_count", 5);
        hairStyleCount = config.getInt("appearance.hair_style_count", 8);
        hairColorCount = config.getInt("appearance.hair_color_count", 6);

        rateLimitCheckNamePerMinute = config.getInt("rate_limit.check_name_per_minute", 5);
    }

    /**
     * Constructeur de test — sans dépendance Bukkit.
     * Seuls les champs utiles à la validation métier sont paramétrables ;
     * les autres prennent leurs valeurs par défaut.
     */
    public PluginConfig(int rpYear, int skinToneCount, int eyeColorCount,
                        int hairStyleCount, int hairColorCount) {
        this.rpYear = rpYear;
        this.skinToneCount = skinToneCount;
        this.eyeColorCount = eyeColorCount;
        this.hairStyleCount = hairStyleCount;
        this.hairColorCount = hairColorCount;
        this.creationWorld = "world";
        this.creationX = 0.5;
        this.creationY = 64.0;
        this.creationZ = 0.5;
        this.creationYaw = 0.0f;
        this.creationTimeoutSeconds = 300;
        this.rateLimitCheckNamePerMinute = 5;
    }

    public int getRpYear() { return rpYear; }
    public String getCreationWorld() { return creationWorld; }
    public double getCreationX() { return creationX; }
    public double getCreationY() { return creationY; }
    public double getCreationZ() { return creationZ; }
    public float getCreationYaw() { return creationYaw; }
    public int getCreationTimeoutSeconds() { return creationTimeoutSeconds; }
    public int getSkinToneCount() { return skinToneCount; }
    public int getEyeColorCount() { return eyeColorCount; }
    public int getHairStyleCount() { return hairStyleCount; }
    public int getHairColorCount() { return hairColorCount; }
    public int getRateLimitCheckNamePerMinute() { return rateLimitCheckNamePerMinute; }
}
