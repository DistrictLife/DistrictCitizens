package dev.districtlife.citizens.model;

public class Appearance {

    private final String uuid;
    private final int skinTone;
    private final int eyeColor;
    private final int hairStyle;
    private final int hairColor;

    public Appearance(String uuid, int skinTone, int eyeColor, int hairStyle, int hairColor) {
        this.uuid = uuid;
        this.skinTone = skinTone;
        this.eyeColor = eyeColor;
        this.hairStyle = hairStyle;
        this.hairColor = hairColor;
    }

    public String getUuid() { return uuid; }
    public int getSkinTone() { return skinTone; }
    public int getEyeColor() { return eyeColor; }
    public int getHairStyle() { return hairStyle; }
    public int getHairColor() { return hairColor; }
}
