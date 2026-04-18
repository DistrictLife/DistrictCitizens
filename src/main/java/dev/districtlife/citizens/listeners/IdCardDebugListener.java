package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.items.IdCardItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener diagnostique temporaire.
 * Logge tout événement qui touche une carte d'identité pour identifier
 * la source de la suppression.
 * Priorité MONITOR = observe sans interférer.
 */
public class IdCardDebugListener implements Listener {

    private final DLCitizensPlugin plugin;

    public IdCardDebugListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Clic dans un inventaire ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor  = event.getCursor();

        boolean cardInSlot   = current != null && IdCardItem.isIdCard(current);
        boolean cardOnCursor = cursor  != null && IdCardItem.isIdCard(cursor);

        if (!cardInSlot && !cardOnCursor) return;

        plugin.getLogger().info(
            "[IdCard DEBUG] InventoryClick"
            + " player="    + event.getWhoClicked().getName()
            + " invType="   + event.getInventory().getType()
            + " slot="      + event.getSlot()
            + " rawSlot="   + event.getRawSlot()
            + " click="     + event.getClick()
            + " action="    + event.getAction()
            + " inSlot="    + cardInSlot
            + " onCursor="  + cardOnCursor
            + " cancelled=" + event.isCancelled()
        );
    }

    // ─── Drag dans un inventaire ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack dragged = event.getOldCursor();
        if (dragged == null || !IdCardItem.isIdCard(dragged)) return;

        plugin.getLogger().info(
            "[IdCard DEBUG] InventoryDrag"
            + " player="    + event.getWhoClicked().getName()
            + " slots="     + event.getRawSlots()
            + " cancelled=" + event.isCancelled()
        );
    }

    // ─── Drop (touche Q ou clic hors fenêtre) ────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        if (!IdCardItem.isIdCard(event.getItemDrop().getItemStack())) return;

        plugin.getLogger().info(
            "[IdCard DEBUG] PlayerDropItem"
            + " player="    + event.getPlayer().getName()
            + " cancelled=" + event.isCancelled()
        );
    }

    // ─── Ramassage d'un item au sol ──────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(PlayerPickupItemEvent event) {
        if (!IdCardItem.isIdCard(event.getItem().getItemStack())) return;

        plugin.getLogger().info(
            "[IdCard DEBUG] PlayerPickupItem"
            + " player="    + event.getPlayer().getName()
            + " cancelled=" + event.isCancelled()
        );
    }

    // ─── Disparition d'un item au sol (despawn) ──────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDespawn(ItemDespawnEvent event) {
        if (!IdCardItem.isIdCard(event.getEntity().getItemStack())) return;

        plugin.getLogger().info(
            "[IdCard DEBUG] ItemDespawn"
            + " loc=" + event.getEntity().getLocation()
            + " cancelled=" + event.isCancelled()
        );
    }
}
