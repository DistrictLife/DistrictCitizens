package dev.districtlife.citizens.network;

import dev.districtlife.citizens.config.PluginConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final int maxCheckNamePerMinute;

    // UUID → [count, windowStartMs]
    private final Map<UUID, long[]> checkNameTokens = new ConcurrentHashMap<>();
    // UUID → nombre de submits effectués cette session
    private final Map<UUID, Integer> submitCounts = new ConcurrentHashMap<>();

    public RateLimiter(PluginConfig config) {
        this.maxCheckNamePerMinute = config.getRateLimitCheckNamePerMinute();
    }

    /**
     * Retourne true si la requête de vérification de nom est autorisée.
     * Token bucket simplifié : fenêtre glissante d'une minute.
     */
    public synchronized boolean checkNameAllowed(UUID uuid) {
        long now = System.currentTimeMillis();
        long[] bucket = checkNameTokens.getOrDefault(uuid, new long[]{0, now});

        // Réinitialiser si fenêtre expirée (> 60s)
        if (now - bucket[1] > 60_000L) {
            bucket[0] = 0;
            bucket[1] = now;
        }

        if (bucket[0] >= maxCheckNamePerMinute) {
            checkNameTokens.put(uuid, bucket);
            return false;
        }

        bucket[0]++;
        checkNameTokens.put(uuid, bucket);
        return true;
    }

    /**
     * Retourne true si le submit est autorisé (max 1 par session).
     */
    public synchronized boolean submitAllowed(UUID uuid) {
        int count = submitCounts.getOrDefault(uuid, 0);
        if (count >= 1) return false;
        submitCounts.put(uuid, count + 1);
        return true;
    }

    /**
     * Réinitialise uniquement le compteur de submit (erreur serveur → l'utilisateur peut réessayer).
     */
    public synchronized void resetSubmit(UUID uuid) {
        submitCounts.remove(uuid);
    }

    /**
     * Réinitialise les compteurs de session. Appelé au PlayerQuitEvent.
     */
    public void resetSession(UUID uuid) {
        checkNameTokens.remove(uuid);
        submitCounts.remove(uuid);
    }
}
