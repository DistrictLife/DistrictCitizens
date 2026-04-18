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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInteractListener implements Listener {

    private final DLCitizensPlugin plugin;

    /**
     * Cooldown par joueur (timestamp ms du dernier clic traité).
     * Empêche les doublons : Arclight/Forge fire PlayerInteractEvent
     * plusieurs fois par clic (main + off-hand + variantes Forge).
     */
    private final ConcurrentHashMap<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 200L;

    public PlayerInteractListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Clic droit dans l'air ou sur un bloc ───────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Bloquer toute interaction pendant le flow de création
        if (plugin.getSessionManager().getSession(uuid) != null) {
            event.setCancelled(true);
            return;
        }

        // Clic droit uniquement
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Récupérer l'item (getItem() retourne null sur Arclight → fallback main hand)
        ItemStack item = event.getItem();
        if (item == null) item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null || !IdCardItem.isIdCard(item)) return;

        // Déduplication : ignorer si un clic a déjà été traité dans les 200 ms
        long now = System.currentTimeMillis();
        if (now - lastClick.getOrDefault(uuid, 0L) < COOLDOWN_MS) {
            event.setCancelled(true);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            return;
        }
        lastClick.put(uuid, now);

        // Annuler l'interaction Bukkit + Forge
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        openIdCard(event.getPlayer(), item);
    }

    // ─── Clic droit sur une entité (autre joueur, PNJ…) ────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (plugin.getSessionManager().getSession(uuid) != null) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null || !IdCardItem.isIdCard(item)) return;

        long now = System.currentTimeMillis();
        if (now - lastClick.getOrDefault(uuid, 0L) < COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        lastClick.put(uuid, now);

        event.setCancelled(true);
        openIdCard(event.getPlayer(), item);
    }

    // ─── Logique commune : lire le PDC, envoyer le packet, restaurer l'item ─

    private void openIdCard(org.bukkit.entity.Player player, ItemStack item) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String serial    = pdc.get(new NamespacedKey(plugin, "id_serial"),     PersistentDataType.STRING);
        String ownerStr  = pdc.get(new NamespacedKey(plugin, "id_owner"),      PersistentDataType.STRING);
        String firstName = pdc.get(new NamespacedKey(plugin, "id_first_name"), PersistentDataType.STRING);
        String lastName  = pdc.get(new NamespacedKey(plugin, "id_last_name"),  PersistentDataType.STRING);
        String birthDate = pdc.get(new NamespacedKey(plugin, "id_birth_date"), PersistentDataType.STRING);

        if (serial == null || ownerStr == null) {
            plugin.getLogger().warning("[IdCard] PDC incomplet — serial=" + serial + " owner=" + ownerStr);
            return;
        }

        UUID ownerUuid;
        try { ownerUuid = UUID.fromString(ownerStr); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[IdCard] UUID invalide : " + ownerStr);
            return;
        }

        final String fn = firstName != null ? firstName : "";
        final String ln = lastName  != null ? lastName  : "";
        final String bd = birthDate != null ? birthDate : "";
        final String sr = serial;
        final UUID   ou = ownerUuid;

        // Clone préventif avant que Forge puisse modifier l'inventaire
        final ItemStack clone = item.clone();

        PacketChannel.sendToPlayer(player, PacketChannel.ID_OPEN_ID_CARD, buf -> {
            BufUtil.writeString(buf, sr, 32);
            BufUtil.writeUUID(buf, ou);
            BufUtil.writeString(buf, fn, 32);
            BufUtil.writeString(buf, ln, 32);
            BufUtil.writeString(buf, bd, 32);
        });

        // Restauration de sécurité : si Forge a consommé la carte malgré le cancel,
        // on la remet dans l'inventaire au tick suivant.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            boolean found = false;
            for (ItemStack s : player.getInventory().getContents()) {
                if (s != null && IdCardItem.isIdCard(s)) { found = true; break; }
            }
            if (!found) {
                player.getInventory().setItemInMainHand(clone);
                plugin.getLogger().warning("[IdCard] Carte restaurée pour " + player.getName()
                    + " (consommée par Forge/mod tiers malgré cancel)");
            }
            player.updateInventory();
        }, 1L);
    }

    // ─── Bloquer uniquement le jet (touche Q) ───────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropItem(PlayerDropItemEvent event) {
        if (IdCardItem.isIdCard(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7cVous ne pouvez pas jeter votre pi\u00e8ce d'identit\u00e9.");
        }
    }
}
