package dev.districtlife.citizens.commands.subcommands;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.UUID;

public class ResetSubCommand {

    private final DLCitizensPlugin plugin;

    public ResetSubCommand(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage : /dlcitizen reset <player>");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO dao = new CitizenDAO(plugin.getDatabaseManager().getConnection());

                if (dao.findByUuid(uuid).isEmpty()) {
                    syncMessage(sender, "§cCitoyen introuvable : §e" + playerName);
                    return;
                }

                dao.deleteByUuid(uuid);
                plugin.getLogger().warning("[DLCITIZENS] RESET effectué sur " + playerName + " (" + uuid + ") par " + sender.getName() + " — ACTION IRRÉVERSIBLE");
                syncMessage(sender, "§aCitoyen §e" + playerName + " §aréinitialisé. Toutes les données ont été supprimées.");

            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur reset : " + e.getMessage());
                syncMessage(sender, "§cErreur interne.");
            }
        });
        return true;
    }

    private void syncMessage(CommandSender sender, String msg) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
    }
}
