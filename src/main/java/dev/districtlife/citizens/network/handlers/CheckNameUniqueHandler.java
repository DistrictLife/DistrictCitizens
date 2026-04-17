package dev.districtlife.citizens.network.handlers;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.network.BufUtil;
import dev.districtlife.citizens.network.PacketChannel;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class CheckNameUniqueHandler {

    private final DLCitizensPlugin plugin;

    public CheckNameUniqueHandler(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    /** Appelé sur le thread Bukkit principal. Les strings sont déjà parsées. */
    public void handle(String firstName, String lastName, Player player) {
        UUID senderUuid = player.getUniqueId();

        if (!plugin.getRateLimiter().checkNameAllowed(senderUuid)) {
            sendResponse(player, false, "Trop de tentatives. Réessayez dans une minute.");
            return;
        }

        if (plugin.getSessionManager().getSession(senderUuid) == null) {
            return;
        }

        try {
            CitizenDAO dao = new CitizenDAO(plugin.getDatabaseManager().getConnection());
            boolean exists = dao.existsByFullName(firstName, lastName);
            if (exists) {
                sendResponse(player, false, "Ce nom est déjà utilisé par un autre citoyen.");
            } else {
                sendResponse(player, true, "");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur CheckNameUnique : " + e.getMessage());
            sendResponse(player, false, "Erreur interne. Réessayez.");
        }
    }

    private void sendResponse(Player player, boolean available, String reason) {
        PacketChannel.sendToPlayer(player, PacketChannel.ID_NAME_CHECK_RESPONSE, buf -> {
            buf.writeBoolean(available);
            BufUtil.writeString(buf, reason, 256);
        });
    }
}
