package dev.districtlife.citizens.commands.subcommands;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

public class StatsSubCommand {

    private final DLCitizensPlugin plugin;

    public StatsSubCommand(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO dao = new CitizenDAO(plugin.getDatabaseManager().getConnection());
                int count = dao.countAll();
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§e[DLCitizens] §fNombre de citoyens enregistrés : §a" + count)
                );
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur stats : " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage("§cErreur lors de la récupération des statistiques.")
                );
            }
        });
        return true;
    }
}
