package dev.districtlife.citizens.commands.subcommands;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.database.dao.CitizenDAO;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.validation.CharacterValidator;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

public class EditSubCommand {

    private final DLCitizensPlugin plugin;

    public EditSubCommand(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        // /dlcitizen edit <player> <field> <value>
        if (args.length < 4) {
            sender.sendMessage("§cUsage : /dlcitizen edit <player> firstname|lastname|birthdate <valeur>");
            return true;
        }

        String playerName = args[1];
        String field = args[2].toLowerCase();
        String value = args[3];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CitizenDAO dao = new CitizenDAO(plugin.getDatabaseManager().getConnection());
                Optional<Citizen> citizenOpt = dao.findByUuid(uuid);

                if (!citizenOpt.isPresent()) {
                    syncMessage(sender, "§cCitoyen introuvable : §e" + playerName);
                    return;
                }

                Citizen c = citizenOpt.get();

                switch (field) {
                    case "firstname": {
                        CharacterValidator.ValidationResult r = CharacterValidator.validateFirstName(value);
                        if (!r.valid()) { syncMessage(sender, "§cPrénom invalide : " + r.errorKey()); return; }
                        String capitalized = CharacterValidator.capitalizeWords(value);
                        if (dao.existsByFullName(capitalized, c.getLastName())) {
                            syncMessage(sender, "§cCe nom complet est déjà utilisé."); return;
                        }
                        dao.updateFirstName(uuid, capitalized);
                        syncMessage(sender, "§aPrénom mis à jour : §f" + capitalized);
                        break;
                    }
                    case "lastname": {
                        CharacterValidator.ValidationResult r = CharacterValidator.validateLastName(value);
                        if (!r.valid()) { syncMessage(sender, "§cNom invalide : " + r.errorKey()); return; }
                        String capitalized = CharacterValidator.capitalizeWords(value);
                        if (dao.existsByFullName(c.getFirstName(), capitalized)) {
                            syncMessage(sender, "§cCe nom complet est déjà utilisé."); return;
                        }
                        dao.updateLastName(uuid, capitalized);
                        syncMessage(sender, "§aNom mis à jour : §f" + capitalized);
                        break;
                    }
                    case "birthdate": {
                        String isoDate;
                        try {
                            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            isoDate = date.toString();
                        } catch (DateTimeParseException e) {
                            syncMessage(sender, "§cFormat de date invalide. Attendu : JJ/MM/AAAA"); return;
                        }
                        CharacterValidator.ValidationResult r = CharacterValidator.validateBirthDate(
                            isoDate, plugin.getPluginConfig().getRpYear());
                        if (!r.valid()) { syncMessage(sender, "§cDate invalide : " + r.errorKey()); return; }
                        dao.updateBirthDate(uuid, isoDate);
                        syncMessage(sender, "§aDate de naissance mise à jour : §f" + isoDate);
                        break;
                    }
                    default:
                        syncMessage(sender, "§cChamp inconnu. Utilisez : firstname, lastname, birthdate");
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur edit : " + e.getMessage());
                syncMessage(sender, "§cErreur interne.");
            }
        });
        return true;
    }

    private void syncMessage(CommandSender sender, String msg) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
    }
}
