package dev.districtlife.citizens.network.handlers;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.AppearanceDAO;
import dev.districtlife.citizens.model.Appearance;
import dev.districtlife.citizens.network.BufUtil;
import dev.districtlife.citizens.network.PacketChannel;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class RequestAppearanceHandler {

    private final DLCitizensPlugin plugin;

    public RequestAppearanceHandler(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    /** Appelé sur le thread Bukkit principal. L'UUID est déjà parsé. */
    public void handle(UUID targetUuid, Player player) {
        try {
            AppearanceDAO dao = new AppearanceDAO(plugin.getDatabaseManager().getConnection());
            Optional<Appearance> appearance = dao.findByUuid(targetUuid);

            appearance.ifPresent(app -> PacketChannel.sendToPlayer(player, PacketChannel.ID_APPEARANCE_SYNC, b -> {
                BufUtil.writeUUID(b, targetUuid);
                b.writeInt(app.getSkinTone());
                b.writeInt(app.getEyeColor());
                b.writeInt(app.getHairStyle());
                b.writeInt(app.getHairColor());
            }));
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur RequestAppearance : " + e.getMessage());
        }
    }
}
