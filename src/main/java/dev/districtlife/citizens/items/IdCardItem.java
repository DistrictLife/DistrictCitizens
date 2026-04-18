package dev.districtlife.citizens.items;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.model.Citizen;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Fabrique l'ItemStack de la pièce d'identité.
 *
 * Stratégie en deux étapes :
 *  1. Crée un ItemStack Bukkit PAPER avec toute la metadata (PDC, display, unbreakable, CMData).
 *  2. Tente de remplacer le type de l'item par « dlclient:id_card » via NMS/Reflection.
 *     → Si dlclient est installé côté serveur (Arclight charge dlclient comme mod Forge),
 *       l'item sera un custom Forge item, ce qui empêche les scripts KubeJS ciblant PAPER
 *       de l'affecter.
 *     → Si dlclient est absent, retourne le PAPER avec PDC (comportement précédent).
 */
public class IdCardItem {

    private static final String PLUGIN_ID = "dlcitizens";

    // ─── Création ─────────────────────────────────────────────────────────────

    public static ItemStack createItemStack(Citizen citizen, String serial) {
        DLCitizensPlugin plugin = DLCitizensPlugin.getInstance();

        // Étape 1 : PAPER avec toute la metadata Bukkit
        ItemStack paper = createPaperWithMeta(citizen, serial, plugin);

        // Étape 2 : tentative de swap vers dlclient:id_card
        try {
            ItemStack custom = swapToDlclientIdCard(paper);
            plugin.getLogger().fine("[IdCard] Item créé en tant que dlclient:id_card");
            return custom;
        } catch (Exception e) {
            plugin.getLogger().warning("[IdCard] dlclient:id_card indisponible, utilisation de PAPER: " + e.getMessage());
            return paper;
        }
    }

    // ─── isIdCard / readSerial ────────────────────────────────────────────────

    /**
     * Vérifie si l'ItemStack est une pièce d'identité, quelle que soit sa base
     * (dlclient:id_card OU PAPER de compatibilité).
     * On ne vérifie plus le type Material — seule la clé PDC fait foi.
     */
    public static boolean isIdCard(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        NamespacedKey serialKey = new NamespacedKey(DLCitizensPlugin.getInstance(), "id_serial");
        return item.getItemMeta().getPersistentDataContainer().has(serialKey, PersistentDataType.STRING);
    }

    public static String readSerial(ItemStack item) {
        NamespacedKey serialKey = new NamespacedKey(DLCitizensPlugin.getInstance(), "id_serial");
        return item.getItemMeta().getPersistentDataContainer().get(serialKey, PersistentDataType.STRING);
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private static ItemStack createPaperWithMeta(Citizen citizen, String serial, DLCitizensPlugin plugin) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("\u00a7e" + citizen.getFirstName() + " " + citizen.getLastName()
            + " \u00a77\u2014 \u00a7fPi\u00e8ce d'identit\u00e9");
        meta.setUnbreakable(true);
        meta.setCustomModelData(1001);

        NamespacedKey serialKey = new NamespacedKey(plugin, "id_serial");
        NamespacedKey ownerKey  = new NamespacedKey(plugin, "id_owner");
        NamespacedKey fnKey     = new NamespacedKey(plugin, "id_first_name");
        NamespacedKey lnKey     = new NamespacedKey(plugin, "id_last_name");
        NamespacedKey bdKey     = new NamespacedKey(plugin, "id_birth_date");

        meta.getPersistentDataContainer().set(serialKey, PersistentDataType.STRING, serial);
        meta.getPersistentDataContainer().set(ownerKey,  PersistentDataType.STRING, citizen.getUuid());
        meta.getPersistentDataContainer().set(fnKey,     PersistentDataType.STRING, citizen.getFirstName());
        meta.getPersistentDataContainer().set(lnKey,     PersistentDataType.STRING, citizen.getLastName());
        meta.getPersistentDataContainer().set(bdKey,     PersistentDataType.STRING, citizen.getBirthDate());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Remplace le type d'un ItemStack Bukkit par « dlclient:id_card » en passant
     * par NMS via réflexion.  Toute la metadata (PDC, display, unbreakable, CMData)
     * est copiée car elle réside dans le CompoundNBT « tag » de l'ItemStack NMS.
     *
     * Chaîne :
     *  1. CraftItemStack.asNMSCopy(bukkit)           → NMS ItemStack (PAPER + tag)
     *  2. ForgeRegistries.ITEMS.getValue(RL)          → Item Forge « dlclient:id_card »
     *  3. new ItemStack(forgeItem, 1)                 → NMS ItemStack vide (dlclient:id_card)
     *  4. paperNms.getTag() → newNms.setTag(tag)      → copie de la metadata
     *  5. CraftItemStack.asBukkitCopy(newNms)         → Bukkit ItemStack final
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack swapToDlclientIdCard(ItemStack bukkit) throws Exception {
        // Classe CraftItemStack (Arclight / CraftBukkit)
        Class<?> craftClass = Class.forName("org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack");

        // 1. Convertir le PAPER Bukkit en NMS ItemStack
        Method asNMSCopy = craftClass.getMethod("asNMSCopy", ItemStack.class);
        Object paperNms = asNMSCopy.invoke(null, bukkit);
        Class<?> nmsStackClass = paperNms.getClass(); // net.minecraft.item.ItemStack

        // 2. Récupérer le CompoundNBT « tag » de l'ItemStack PAPER (contient PDC, display, etc.)
        Object tag = nmsStackClass.getMethod("getTag").invoke(paperNms);

        // 3. ForgeRegistries.ITEMS → Item « dlclient:id_card »
        Class<?> forgeRegClass  = Class.forName("net.minecraftforge.registries.ForgeRegistries");
        Object   itemsRegistry  = forgeRegClass.getField("ITEMS").get(null);
        Class<?> rlClass        = Class.forName("net.minecraft.util.ResourceLocation");
        Object   rl             = rlClass.getConstructor(String.class, String.class)
                                         .newInstance("dlclient", "id_card");
        Object   forgeItem      = itemsRegistry.getClass()
                                               .getMethod("getValue", rlClass)
                                               .invoke(itemsRegistry, rl);

        if (forgeItem == null) {
            throw new IllegalStateException("dlclient:id_card non trouvé dans ForgeRegistries.ITEMS");
        }

        // 4. Créer un nouvel NMS ItemStack de type dlclient:id_card (count = 1)
        Object newNms = null;
        for (Constructor<?> ctor : nmsStackClass.getConstructors()) {
            Class<?>[] types = ctor.getParameterTypes();
            if (types.length == 2 && types[1] == int.class) {
                // (IItemProvider, int)
                newNms = ctor.newInstance(forgeItem, 1);
                break;
            }
        }
        if (newNms == null) {
            // Fallback : constructeur à 1 paramètre
            for (Constructor<?> ctor : nmsStackClass.getConstructors()) {
                if (ctor.getParameterCount() == 1) {
                    newNms = ctor.newInstance(forgeItem);
                    break;
                }
            }
        }
        if (newNms == null) throw new IllegalStateException("Aucun constructeur ItemStack(IItemProvider) trouvé");

        // 5. Copier le tag (metadata : PDC, display, unbreakable, CMData) sur le nouvel item
        if (tag != null) {
            newNms.getClass().getMethod("setTag", tag.getClass()).invoke(newNms, tag);
        }

        // 6. Convertir en Bukkit ItemStack
        Method asBukkitCopy = null;
        for (Method m : craftClass.getDeclaredMethods()) {
            if ("asBukkitCopy".equals(m.getName()) && m.getParameterCount() == 1) {
                asBukkitCopy = m;
                break;
            }
        }
        if (asBukkitCopy == null) throw new IllegalStateException("CraftItemStack.asBukkitCopy introuvable");
        asBukkitCopy.setAccessible(true);
        return (ItemStack) asBukkitCopy.invoke(null, newNms);
    }
}
