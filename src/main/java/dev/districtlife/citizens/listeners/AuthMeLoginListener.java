package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.AppearanceDAO;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.model.Appearance;
import dev.districtlife.citizens.network.BufUtil;
import dev.districtlife.citizens.network.PacketChannel;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class AuthMeLoginListener implements Listener {

    private final DLCitizensPlugin plugin;

    public AuthMeLoginListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO citizenDao = new CitizenDAO(plugin.getDatabaseManager().getConnection());

                if (citizenDao.findByUuid(uuid).isPresent()) {
                    // Joueur déjà citoyen : sync apparence à tous
                    AppearanceDAO appearanceDao = new AppearanceDAO(plugin.getDatabaseManager().getConnection());
                    Optional<Appearance> appearance = appearanceDao.findByUuid(uuid);
                    appearance.ifPresent(app -> plugin.getServer().getScheduler().runTask(plugin, () ->
                        broadcastAppearanceSync(uuid, app)
                    ));
                    return;
                }

                // Pas encore citoyen : déclencher le flow
                plugin.getServer().getScheduler().runTask(plugin, () -> triggerCreationFlow(player));
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur AuthMeLoginListener : " + e.getMessage());
            }
        });
    }

    private void triggerCreationFlow(Player player) {
        UUID uuid = player.getUniqueId();

        if (plugin.getSessionManager().getSession(uuid) != null) {
            return; // Session déjà active
        }

        plugin.getSessionManager().createSession(uuid);

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

        PacketChannel.sendToPlayer(player, PacketChannel.ID_OPEN_CHARACTER_CREATION, buf -> {});
        PacketChannel.sendToPlayer(player, PacketChannel.ID_APPEARANCE_CONFIG, buf -> {
            buf.writeInt(plugin.getPluginConfig().getSkinToneCount());
            buf.writeInt(plugin.getPluginConfig().getEyeColorCount());
            buf.writeInt(plugin.getPluginConfig().getHairStyleCount());
            buf.writeInt(plugin.getPluginConfig().getHairColorCount());
            buf.writeInt(plugin.getPluginConfig().getRpYear());
        });
    }

    private void broadcastAppearanceSync(UUID uuid, Appearance app) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            PacketChannel.sendToPlayer(online, PacketChannel.ID_APPEARANCE_SYNC, buf -> {
                BufUtil.writeUUID(buf, uuid);
                buf.writeInt(app.getSkinTone());
                buf.writeInt(app.getEyeColor());
                buf.writeInt(app.getHairStyle());
                buf.writeInt(app.getHairColor());
            });
        }
    }
}
