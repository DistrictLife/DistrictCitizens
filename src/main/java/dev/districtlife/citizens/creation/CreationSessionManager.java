package dev.districtlife.citizens.creation;

import dev.districtlife.citizens.DLCitizensPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CreationSessionManager {

    private final DLCitizensPlugin plugin;
    private final Map<UUID, CreationSession> sessions = new ConcurrentHashMap<>();

    public CreationSessionManager(DLCitizensPlugin plugin) {
        this.plugin = plugin;
        startExpirationScheduler();
    }

    public CreationSession createSession(UUID uuid) {
        CreationSession session = new CreationSession(uuid);
        sessions.put(uuid, session);
        return session;
    }

    public CreationSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }

    private void startExpirationScheduler() {
        // Vérifie toutes les 30 secondes les sessions expirées
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int timeout = plugin.getPluginConfig().getCreationTimeoutSeconds();
            for (Map.Entry<UUID, CreationSession> entry : sessions.entrySet()) {
                if (entry.getValue().isExpired(timeout)) {
                    sessions.remove(entry.getKey());
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.kickPlayer("§cLa création de personnage a expiré. Reconnecte-toi.");
                    }
                }
            }
        }, 600L, 600L); // 600 ticks = 30 secondes
    }
}
