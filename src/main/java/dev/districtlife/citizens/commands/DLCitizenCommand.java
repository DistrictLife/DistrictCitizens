package dev.districtlife.citizens.commands;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.commands.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DLCitizenCommand implements CommandExecutor, TabCompleter {

    private final DLCitizensPlugin plugin;
    private final LookupSubCommand lookup;
    private final EditSubCommand edit;
    private final ResetSubCommand reset;
    private final ReissueSubCommand reissue;
    private final StatsSubCommand stats;

    public DLCitizenCommand(DLCitizensPlugin plugin) {
        this.plugin = plugin;
        this.lookup  = new LookupSubCommand(plugin);
        this.edit    = new EditSubCommand(plugin);
        this.reset   = new ResetSubCommand(plugin);
        this.reissue = new ReissueSubCommand(plugin);
        this.stats   = new StatsSubCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dlcitizens.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "lookup":  return lookup.execute(sender, args);
            case "edit":    return edit.execute(sender, args);
            case "reset":   return reset.execute(sender, args);
            case "reissue": return reissue.execute(sender, args);
            case "stats":   return stats.execute(sender, args);
            default:        sendUsage(sender); return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dlcitizens.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("lookup", "edit", "reset", "reissue", "stats");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("stats")) {
            return plugin.getServer().getOnlinePlayers().stream()
                .map(p -> p.getName())
                .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            return Arrays.asList("firstname", "lastname", "birthdate");
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/dlcitizen §7lookup|edit|reset|reissue|stats");
    }
}
