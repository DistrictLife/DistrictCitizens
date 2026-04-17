package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final DLCitizensPlugin plugin;

    public PlayerChatListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getSessionManager().getSession(event.getPlayer().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}
