package dev.districtlife.citizens.model;

public class Citizen {

    private final String uuid;
    private final String minecraftName;
    private final String firstName;
    private final String lastName;
    private final String birthDate;
    private final long registeredAt;

    public Citizen(String uuid, String minecraftName, String firstName,
                   String lastName, String birthDate, long registeredAt) {
        this.uuid = uuid;
        this.minecraftName = minecraftName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.registeredAt = registeredAt;
    }

    public String getUuid() { return uuid; }
    public String getMinecraftName() { return minecraftName; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getBirthDate() { return birthDate; }
    public long getRegisteredAt() { return registeredAt; }
}
