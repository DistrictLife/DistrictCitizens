package dev.districtlife.citizens.network.handlers;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.items.IdCardItem;
import dev.districtlife.citizens.model.Appearance;
import dev.districtlife.citizens.model.Citizen;
import dev.districtlife.citizens.network.BufUtil;
import dev.districtlife.citizens.network.PacketChannel;
import dev.districtlife.citizens.transaction.CharacterCreationTransaction;
import dev.districtlife.citizens.validation.CharacterValidator;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.UUID;

public class SubmitCharacterHandler {

    private final DLCitizensPlugin plugin;

    public SubmitCharacterHandler(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    /** Appelé sur le thread Bukkit principal. Toutes les données sont déjà parsées. */
    public void handle(String firstName, String lastName, String birthDate,
                       int skinTone, int eyeColor, int hairStyle, int hairColor,
                       Player player) {

        UUID senderUuid = player.getUniqueId();
        plugin.getLogger().info("[SubmitChar] Traitement pour " + player.getName()
            + " fn=" + firstName + " ln=" + lastName + " bd=" + birthDate
            + " st=" + skinTone + " ec=" + eyeColor + " hs=" + hairStyle + " hc=" + hairColor);

        if (!plugin.getRateLimiter().submitAllowed(senderUuid)) {
            plugin.getLogger().info("[SubmitChar] Rate limit atteint pour " + player.getName());
            sendFailed(player, 3, "Vous avez déjà soumis une demande.");
            return;
        }

        if (plugin.getSessionManager().getSession(senderUuid) == null) {
            plugin.getLogger().warning("[SubmitChar] Session introuvable pour " + player.getName()
                + " — envoi d'une erreur au lieu de retour silencieux.");
            sendFailed(player, 1, "Session expirée. Reconnecte-toi.");
            return;
        }
        plugin.getLogger().info("[SubmitChar] Session OK");

        CharacterValidator.ValidationResult fnResult = CharacterValidator.validateFirstName(firstName);
        if (!fnResult.valid()) {
            plugin.getLogger().info("[SubmitChar] Validation prénom échouée : " + fnResult.errorKey());
            sendFailed(player, 1, translate(fnResult.errorKey())); return;
        }

        CharacterValidator.ValidationResult lnResult = CharacterValidator.validateLastName(lastName);
        if (!lnResult.valid()) {
            plugin.getLogger().info("[SubmitChar] Validation nom échouée : " + lnResult.errorKey());
            sendFailed(player, 1, translate(lnResult.errorKey())); return;
        }

        CharacterValidator.ValidationResult bdResult = CharacterValidator.validateBirthDate(birthDate, plugin.getPluginConfig().getRpYear());
        if (!bdResult.valid()) {
            plugin.getLogger().info("[SubmitChar] Validation date échouée : " + bdResult.errorKey()
                + " (rpYear=" + plugin.getPluginConfig().getRpYear() + ")");
            sendFailed(player, 1, translate(bdResult.errorKey())); return;
        }

        CharacterValidator.ValidationResult appResult = CharacterValidator.validateAppearance(
            skinTone, eyeColor, hairStyle, hairColor, plugin.getPluginConfig()
        );
        if (!appResult.valid()) {
            plugin.getLogger().info("[SubmitChar] Validation apparence échouée : " + appResult.errorKey());
            sendFailed(player, 2, translate(appResult.errorKey())); return;
        }
        plugin.getLogger().info("[SubmitChar] Toutes les validations OK, création du personnage...");

        Citizen citizen = new Citizen(
            senderUuid.toString(),
            player.getName(),
            CharacterValidator.capitalizeWords(firstName),
            CharacterValidator.capitalizeWords(lastName),
            birthDate,
            System.currentTimeMillis()
        );
        Appearance appearance = new Appearance(
            senderUuid.toString(), skinTone, eyeColor, hairStyle, hairColor
        );

        try {
            CharacterCreationTransaction transaction = new CharacterCreationTransaction(plugin);
            String serial = transaction.createCharacter(citizen, appearance, plugin.getPluginConfig().getRpYear());
            plugin.getLogger().info("[SubmitChar] Transaction OK, serial=" + serial);

            ItemStack idCard = IdCardItem.createItemStack(citizen, serial);
            player.getInventory().setItem(0, idCard);
            plugin.getLogger().info("[SubmitChar] Carte d'identité créée et donnée");

            player.setInvulnerable(false);
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getSessionManager().removeSession(senderUuid);
            plugin.getLogger().info("[SubmitChar] Session supprimée, gamemode libéré");

            broadcastAppearanceSync(senderUuid, skinTone, eyeColor, hairStyle, hairColor);

            plugin.getLogger().info("[SubmitChar] Envoi CharacterCreated à " + player.getName());
            PacketChannel.sendToPlayer(player, PacketChannel.ID_CHARACTER_CREATED, buf -> {
                BufUtil.writeString(buf, serial, 32);
                BufUtil.writeString(buf, citizen.getFirstName(), 32);
                BufUtil.writeString(buf, citizen.getLastName(), 32);
            });
            plugin.getLogger().info("[SubmitChar] CharacterCreated envoyé avec succès");

        } catch (SQLException e) {
            plugin.getLogger().severe("[SubmitChar] SQLException : " + e.getMessage());
            plugin.getRateLimiter().resetSubmit(senderUuid);
            if (e.getMessage() != null && e.getMessage().contains("race_condition")) {
                sendFailed(player, 1, "Ce nom est déjà pris. Choisissez un autre.");
            } else {
                sendFailed(player, 1, "Erreur interne. Réessayez.");
            }
        } catch (Throwable e) {
            plugin.getLogger().severe("[SubmitChar] Erreur inattendue : " + e);
            e.printStackTrace();
            plugin.getRateLimiter().resetSubmit(senderUuid);
            sendFailed(player, 1, "Erreur interne. Réessayez.");
        }
    }

    private void sendFailed(Player player, int step, String message) {
        PacketChannel.sendToPlayer(player, PacketChannel.ID_CHARACTER_CREATION_FAILED, buf -> {
            buf.writeInt(step);
            BufUtil.writeString(buf, message, 256);
        });
    }

    private void broadcastAppearanceSync(UUID uuid, int skinTone, int eyeColor, int hairStyle, int hairColor) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            PacketChannel.sendToPlayer(online, PacketChannel.ID_APPEARANCE_SYNC, buf -> {
                BufUtil.writeUUID(buf, uuid);
                buf.writeInt(skinTone);
                buf.writeInt(eyeColor);
                buf.writeInt(hairStyle);
                buf.writeInt(hairColor);
            });
        }
    }

    private String translate(String key) {
        switch (key) {
            case "error.name.too_short":          return "Le prénom/nom est trop court (min 3 caractères).";
            case "error.name.too_long":           return "Le prénom/nom est trop long (max 16 caractères).";
            case "error.name.invalid_chars":      return "Le prénom/nom contient des caractères invalides.";
            case "error.birthdate.invalid":       return "La date de naissance est invalide.";
            case "error.birthdate.too_young":     return "Le personnage doit avoir au moins 18 ans.";
            case "error.birthdate.too_old":       return "Le personnage ne peut pas avoir plus de 90 ans.";
            case "error.appearance.out_of_range": return "Les valeurs d'apparence sont hors limites.";
            default:                              return "Données invalides.";
        }
    }
}
