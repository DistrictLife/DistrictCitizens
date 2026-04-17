package dev.districtlife.citizens.commands.subcommands;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.database.dao.IdCardDAO;
import dev.districtlife.citizens.items.IdCardItem;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.model.IdCard;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class ReissueSubCommand {

    private final DLCitizensPlugin plugin;

    public ReissueSubCommand(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage : /dlcitizen reissue <player>");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO citizenDao = new CitizenDAO(plugin.getDatabaseManager().getConnection());
                IdCardDAO idCardDao = new IdCardDAO(plugin.getDatabaseManager().getConnection());

                Optional<Citizen> citizenOpt = citizenDao.findByUuid(uuid);
                if (citizenOpt.isEmpty()) {
                    syncMessage(sender, "§cCitoyen introuvable : §e" + playerName); return;
                }

                Optional<IdCard> cardOpt = idCardDao.findByOwnerUuid(uuid);
                if (cardOpt.isEmpty()) {
                    syncMessage(sender, "§cAucune carte d'identité trouvée pour §e" + playerName); return;
                }

                Citizen citizen = citizenOpt.get();
                String serial = cardOpt.get().getSerial();

                idCardDao.incrementReissueCount(serial);
                ItemStack idCard = IdCardItem.createItemStack(citizen, serial);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player online = plugin.getServer().getPlayer(uuid);
                    if (online != null && online.isOnline()) {
                        online.getInventory().addItem(idCard);
                        sender.sendMessage("§aCarte d'identité §e" + serial + " §areémise à §e" + playerName + "§a.");
                    } else {
                        plugin.getLogger().warning("[DLCITIZENS] Reissue pour " + playerName + " (" + serial + ") : joueur hors ligne, item non donné.");
                        sender.sendMessage("§aCarte d'identité §e" + serial + " §agénérée mais §e" + playerName + " §aest hors ligne. Item non donné.");
                    }
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur reissue : " + e.getMessage());
                syncMessage(sender, "§cErreur interne.");
            }
        });
        return true;
    }

    private void syncMessage(CommandSender sender, String msg) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
    }
}
