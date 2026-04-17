package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.network.PacketChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Kick players who connected without the dlclient mod.
 *
 * Since Forge's channel handshake happens before any Bukkit event,
 * we accept ABSENT in the channel predicate and do the check here.
 */
public class ModCheckListener implements Listener {

    private final DLCitizensPlugin plugin;

    public ModCheckListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Run on next tick so the connection is fully established
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            // Admins with bypass permission can connect without the mod (e.g. for testing)
            if (player.hasPermission("dlcitizens.bypass.modcheck")) return;
            if (!hasDistrictLifeMod(player)) {
                player.kickPlayer(
                    "§c§lDistrict Life — Mod requis\n" +
                    "§r\n" +
                    "§fLe mod §edlclient §fest obligatoire pour jouer sur ce serveur.\n" +
                    "§7Installe le modpack District Life et reconnecte-toi."
                );
            }
        });
    }

    /**
     * Returns true if the player's client has the districtlife:main channel
     * registered at the correct protocol version.
     *
     * On Arclight (Forge+Bukkit hybrid), we reach into the NMS player to get
     * the Forge NetworkManager and query the SimpleChannel.
     */
    private boolean hasDistrictLifeMod(Player player) {
        try {
            // CraftPlayer → NMS ServerPlayerEntity
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);

            // ServerPlayerEntity.connection → ServerGamePacketListenerImpl
            Object connection = nmsPlayer.getClass().getField("connection").get(nmsPlayer);

            // ServerGamePacketListenerImpl.connection → NetworkManager
            Object networkManager = connection.getClass()
                .getField("connection")
                .get(connection);

            // Ask Forge: is this channel present on the remote side?
            // NetworkHooks.isVanillaConnection → false means Forge client
            // but we need to check specifically for our channel version.
            // Use the SimpleChannel's isPresent method (Forge 36.x).
            java.lang.reflect.Method isPresent = PacketChannel.getChannel()
                .getClass()
                .getMethod("isPresent", networkManager.getClass().getSuperclass());

            return (boolean) isPresent.invoke(PacketChannel.getChannel(), networkManager);

        } catch (NoSuchMethodException e) {
            // Fallback: try with exact class
            return hasDistrictLifeModFallback(player);
        } catch (Exception e) {
            plugin.getLogger().warning(
                "ModCheckListener: could not check mod presence for " +
                player.getName() + " — " + e.getMessage()
            );
            // Allow connection on error to avoid false kicks
            return true;
        }
    }

    private boolean hasDistrictLifeModFallback(Player player) {
        try {
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getField("connection").get(nmsPlayer);
            Object networkManager = connection.getClass()
                .getField("connection")
                .get(connection);

            // Walk all declared methods on SimpleChannel to find isPresent
            for (java.lang.reflect.Method m : PacketChannel.getChannel().getClass().getMethods()) {
                if (m.getName().equals("isPresent") && m.getParameterCount() == 1) {
                    return (boolean) m.invoke(PacketChannel.getChannel(), networkManager);
                }
            }
            return true; // not found, allow
        } catch (Exception e) {
            return true;
        }
    }
}
