package dev.districtlife.citizens.creation;

import java.util.UUID;

public class CreationSession {

    private final UUID uuid;
    private final long startTime;
    private int currentStep;

    public CreationSession(UUID uuid) {
        this.uuid = uuid;
        this.startTime = System.currentTimeMillis();
        this.currentStep = 1;
    }

    public UUID getUuid() { return uuid; }
    public long getStartTime() { return startTime; }
    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int step) { this.currentStep = step; }

    public boolean isExpired(int timeoutSeconds) {
        return System.currentTimeMillis() - startTime > (long) timeoutSeconds * 1000L;
    }
}
