package dev.districtlife.citizens.items;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.model.Citizen;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class IdCardItem {

    private static final String PLUGIN_ID = "dlcitizens";

    public static ItemStack createItemStack(Citizen citizen, String serial) {
        DLCitizensPlugin plugin = DLCitizensPlugin.getInstance();

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("\u00a7e" + citizen.getFirstName() + " " + citizen.getLastName()
            + " \u00a77\u2014 \u00a7fPi\u00e8ce d'identit\u00e9");
        meta.setUnbreakable(true);
        meta.setCustomModelData(1001);

        NamespacedKey serialKey = new NamespacedKey(plugin, "id_serial");
        NamespacedKey ownerKey = new NamespacedKey(plugin, "id_owner");
        NamespacedKey fnKey = new NamespacedKey(plugin, "id_first_name");
        NamespacedKey lnKey = new NamespacedKey(plugin, "id_last_name");
        NamespacedKey bdKey = new NamespacedKey(plugin, "id_birth_date");

        meta.getPersistentDataContainer().set(serialKey, PersistentDataType.STRING, serial);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, citizen.getUuid());
        meta.getPersistentDataContainer().set(fnKey, PersistentDataType.STRING, citizen.getFirstName());
        meta.getPersistentDataContainer().set(lnKey, PersistentDataType.STRING, citizen.getLastName());
        meta.getPersistentDataContainer().set(bdKey, PersistentDataType.STRING, citizen.getBirthDate());

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isIdCard(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData() || meta.getCustomModelData() != 1001) return false;
        NamespacedKey serialKey = new NamespacedKey(DLCitizensPlugin.getInstance(), "id_serial");
        return meta.getPersistentDataContainer().has(serialKey, PersistentDataType.STRING);
    }

    public static String readSerial(ItemStack item) {
        NamespacedKey serialKey = new NamespacedKey(DLCitizensPlugin.getInstance(), "id_serial");
        return item.getItemMeta().getPersistentDataContainer().get(serialKey, PersistentDataType.STRING);
    }
}
