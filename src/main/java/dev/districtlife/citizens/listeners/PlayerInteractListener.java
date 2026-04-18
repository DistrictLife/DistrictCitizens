package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class PlayerInteractListener implements Listener {

    private final DLCitizensPlugin plugin;

    public PlayerInteractListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Bloquer toute interaction pendant le flow de création ──────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getSessionManager().getSession(uuid) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getSessionManager().getSession(uuid) != null) {
            event.setCancelled(true);
        }
    }
}
