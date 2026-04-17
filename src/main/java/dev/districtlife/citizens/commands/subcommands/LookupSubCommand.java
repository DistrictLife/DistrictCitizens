package dev.districtlife.citizens.commands.subcommands;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.database.dao.IdCardDAO;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.model.IdCard;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class LookupSubCommand {

    private final DLCitizensPlugin plugin;

    public LookupSubCommand(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage : /dlcitizen lookup <player>");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO citizenDao = new CitizenDAO(plugin.getDatabaseManager().getConnection());
                IdCardDAO idCardDao = new IdCardDAO(plugin.getDatabaseManager().getConnection());

                Optional<Citizen> citizenOpt = citizenDao.findByUuid(target.getUniqueId());
                if (citizenOpt.isEmpty()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§cAucun citoyen trouvé pour §e" + playerName)
                    );
                    return;
                }

                Citizen c = citizenOpt.get();
                Optional<IdCard> cardOpt = idCardDao.findByOwnerUuid(target.getUniqueId());
                String serial = cardOpt.map(IdCard::getSerial).orElse("N/A");
                String registeredDate = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(c.getRegisteredAt()));

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§e--- Citoyen : " + c.getMinecraftName() + " ---");
                    sender.sendMessage("§7Prénom : §f" + c.getFirstName());
                    sender.sendMessage("§7Nom : §f" + c.getLastName());
                    sender.sendMessage("§7Date de naissance : §f" + c.getBirthDate());
                    sender.sendMessage("§7UUID : §f" + c.getUuid());
                    sender.sendMessage("§7Serial carte : §f" + serial);
                    sender.sendMessage("§7Minecraft name : §f" + c.getMinecraftName());
                    sender.sendMessage("§7Enregistré le : §f" + registeredDate);
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lookup : " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§cErreur interne lors de la recherche.")
                );
            }
        });
        return true;
    }
}
