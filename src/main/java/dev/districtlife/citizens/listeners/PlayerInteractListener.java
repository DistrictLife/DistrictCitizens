package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.items.IdCardItem;
import dev.districtlife.citizens.network.BufUtil;
import dev.districtlife.citizens.network.PacketChannel;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class PlayerInteractListener implements Listener {

    private final DLCitizensPlugin plugin;

    public PlayerInteractListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Bloquer toute interaction pendant le flow de création
        if (plugin.getSessionManager().getSession(uuid) != null) {
            event.setCancelled(true);
            return;
        }

        // Carte d'identité : uniquement sur clic droit
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // getItem() peut retourner null sur Arclight — fallback sur la main principale
        ItemStack item = event.getItem();
        if (item == null) {
            item = event.getPlayer().getInventory().getItemInMainHand();
        }

        plugin.getLogger().info("[IdCard] RIGHT_CLICK: player=" + event.getPlayer().getName()
            + " item=" + (item != null ? item.getType().name() : "null")
            + " hasMeta=" + (item != null && item.hasItemMeta())
            + " isIdCard=" + (item != null && IdCardItem.isIdCard(item)));

        if (item == null || !IdCardItem.isIdCard(item)) return;

        event.setCancelled(true);
        event.getPlayer().updateInventory();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning("[IdCard] ItemMeta null pour " + event.getPlayer().getName());
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String serial    = pdc.get(new NamespacedKey(plugin, "id_serial"),     PersistentDataType.STRING);
        String ownerStr  = pdc.get(new NamespacedKey(plugin, "id_owner"),      PersistentDataType.STRING);
        String firstName = pdc.get(new NamespacedKey(plugin, "id_first_name"), PersistentDataType.STRING);
        String lastName  = pdc.get(new NamespacedKey(plugin, "id_last_name"),  PersistentDataType.STRING);
        String birthDate = pdc.get(new NamespacedKey(plugin, "id_birth_date"), PersistentDataType.STRING);

        plugin.getLogger().info("[IdCard] PDC lu : serial=" + serial + " owner=" + ownerStr
            + " fn=" + firstName + " ln=" + lastName + " bd=" + birthDate);

        if (serial == null || ownerStr == null) {
            plugin.getLogger().warning("[IdCard] PDC incomplet (serial=" + serial + " owner=" + ownerStr + ")");
            return;
        }

        UUID ownerUuid;
        try {
            ownerUuid = UUID.fromString(ownerStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[IdCard] UUID invalide : " + ownerStr);
            return;
        }

        final String fn = firstName != null ? firstName : "";
        final String ln = lastName  != null ? lastName  : "";
        final String bd = birthDate != null ? birthDate : "";
        final String sr = serial;
        final UUID   ou = ownerUuid;

        plugin.getLogger().info("[IdCard] Envoi OpenIdCardPacket \u00e0 " + event.getPlayer().getName());
        PacketChannel.sendToPlayer(event.getPlayer(), PacketChannel.ID_OPEN_ID_CARD, buf -> {
            BufUtil.writeString(buf, sr, 32);
            BufUtil.writeUUID(buf, ou);
            BufUtil.writeString(buf, fn, 32);
            BufUtil.writeString(buf, ln, 32);
            BufUtil.writeString(buf, bd, 32);
        });
    }

    // Empêche le joueur de jeter la carte d'identité (touche Q)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropItem(PlayerDropItemEvent event) {
        if (IdCardItem.isIdCard(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
            event.getPlayer().sendMessage("\u00a7cVous ne pouvez pas jeter votre pi\u00e8ce d'identit\u00e9.");
        }
    }

    // Empêche le joueur de déplacer la carte d'identité dans l'inventaire
    // (évite la désync client/serveur qui fait disparaître l'item)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor  = event.getCursor();

        boolean currentIsCard = IdCardItem.isIdCard(current);
        boolean cursorIsCard  = IdCardItem.isIdCard(cursor);

        if (currentIsCard || cursorIsCard) {
            event.setCancelled(true);
            event.getWhoClicked().updateInventory();
        }
    }
}
