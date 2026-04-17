package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.network.PacketChannel;
import fr.xephi.authme.events.RegisterEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.sql.SQLException;
import java.util.UUID;

public class AuthMeRegisterListener implements Listener {

    private final DLCitizensPlugin plugin;

    public AuthMeRegisterListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRegister(RegisterEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO dao = new CitizenDAO(plugin.getDatabaseManager().getConnection());
                if (dao.findByUuid(uuid).isPresent()) {
                    // Déjà un citoyen — ignorer
                    return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> triggerCreationFlow(player));
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur AuthMeRegisterListener : " + e.getMessage());
            }
        });
    }

    private void triggerCreationFlow(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getSessionManager().createSession(uuid);

        // Téléporter à la zone de création
        World world = plugin.getServer().getWorld(plugin.getPluginConfig().getCreationWorld());
        if (world != null) {
            Location loc = new Location(
                world,
                plugin.getPluginConfig().getCreationX(),
                plugin.getPluginConfig().getCreationY(),
                plugin.getPluginConfig().getCreationZ(),
                plugin.getPluginConfig().getCreationYaw(),
                0f
            );
            player.teleport(loc);
        }

        player.setInvulnerable(true);
        player.setGameMode(GameMode.ADVENTURE);

        // Envoyer le packet d'ouverture
        PacketChannel.sendToPlayer(player, PacketChannel.ID_OPEN_CHARACTER_CREATION, buf -> {});

        // Envoyer la config d'apparence (counts + rpYear)
        PacketChannel.sendToPlayer(player, PacketChannel.ID_APPEARANCE_CONFIG, buf -> {
            buf.writeInt(plugin.getPluginConfig().getSkinToneCount());
            buf.writeInt(plugin.getPluginConfig().getEyeColorCount());
            buf.writeInt(plugin.getPluginConfig().getHairStyleCount());
            buf.writeInt(plugin.getPluginConfig().getHairColorCount());
            buf.writeInt(plugin.getPluginConfig().getRpYear());
        });
    }
}
