package dev.districtlife.citizens.listeners;

import dev.districtlife.citizens.DLCitizensPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Enregistre des listeners Forge (MinecraftForge.EVENT_BUS) à priorité HIGHEST
 * pour protéger la pièce d'identité contre toute logique de consommation externe
 * (KubeJS, autre mod…) avant que Bukkit ne reçoive les événements.
 *
 * Sur Arclight, les classes Forge sont accessibles via Class.forName() depuis un plugin.
 * Tous les appels Forge passent par réflexion pour éviter des dépendances de compilation.
 *
 * Événements couverts :
 *  - ItemTossEvent              → annule le jet (touche Q)
 *  - PlayerInteractEvent$RightClickItem  → annule l'utilisation (clic droit dans l'air)
 *  - PlayerInteractEvent$RightClickBlock → annule l'utilisation sur un bloc
 *  - PlayerInteractEvent$EntityInteract  → annule l'interaction avec une entité
 *  - PlayerInteractEvent$LeftClickEmpty  → annule le clic gauche dans l'air
 *  - PlayerInteractEvent$LeftClickBlock  → annule le clic gauche sur un bloc
 *
 * Après chaque annulation, updateInventory() est planifié sur le thread Bukkit pour
 * forcer le renvoi de l'inventaire au client et annuler toute prédiction côté client.
 */
public class ForgeProtectionListener {

    private final DLCitizensPlugin plugin;
    private final Logger log;

    public ForgeProtectionListener(DLCitizensPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
    }

    // ─── Enregistrement ──────────────────────────────────────────────────────

    public void register() {
        try {
            Class<?> mcForge      = Class.forName("net.minecraftforge.common.MinecraftForge");
            Object   eventBus     = mcForge.getField("EVENT_BUS").get(null);
            Class<?> priorityCls  = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
            Object   highest      = priorityCls.getField("HIGHEST").get(null);

            // Jet d'item (Q)
            registerForgeEvent(eventBus, highest,
                "net.minecraftforge.event.entity.player.ItemTossEvent",
                this::handleItemToss);

            // Clic droit dans l'air
            registerForgeEvent(eventBus, highest,
                "net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem",
                this::handleInteract);

            // Clic droit sur un bloc
            registerForgeEvent(eventBus, highest,
                "net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock",
                this::handleInteract);

            // Clic droit sur une entité
            registerForgeEvent(eventBus, highest,
                "net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract",
                this::handleInteract);

            // Clic gauche dans l'air
            registerForgeEvent(eventBus, highest,
                "net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickEmpty",
                this::handleInteract);

            // Clic gauche sur un bloc
            registerForgeEvent(eventBus, highest,
                "net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickBlock",
                this::handleInteract);

            log.info("[IdCard] Protection Forge enregistrée (6 événements).");

        } catch (Exception e) {
            log.warning("[IdCard] Impossible d'enregistrer la protection Forge: " + e);
        }
    }

    // ─── Handlers ─────────────────────────────────────────────────────────────

    /** Annule le jet de pièce d'identité (touche Q). */
    private void handleItemToss(Object event) {
        try {
            Object entityItem = event.getClass().getMethod("getEntityItem").invoke(event);
            Object nmsStack   = entityItem.getClass().getMethod("getItem").invoke(entityItem);
            if (!isIdCardNms(nmsStack)) return;

            cancelEvent(event);
            log.fine("[IdCard] ItemTossEvent annulé.");

            // Récupère le joueur via getPlayer() sur l'événement PlayerEvent parent
            Object nmsPlayer = event.getClass().getMethod("getPlayer").invoke(event);
            scheduleInventoryUpdate(nmsPlayer);

        } catch (Exception e) {
            log.warning("[IdCard] handleItemToss: " + e);
        }
    }

    /** Annule toute interaction Forge (RightClick*, LeftClick*) impliquant une carte. */
    private void handleInteract(Object event) {
        try {
            Object nmsStack = event.getClass().getMethod("getItemStack").invoke(event);
            if (!isIdCardNms(nmsStack)) return;

            cancelEvent(event);

            // Tente de forcer le résultat à FAIL (évite la prédiction côté client)
            trySetCancellationResultFail(event);

            log.fine("[IdCard] PlayerInteractEvent (" + event.getClass().getSimpleName() + ") annulé.");

            Object nmsPlayer = event.getClass().getMethod("getPlayer").invoke(event);
            scheduleInventoryUpdate(nmsPlayer);

        } catch (Exception e) {
            log.warning("[IdCard] handleInteract (" + event.getClass().getSimpleName() + "): " + e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Vérifie si un NMS ItemStack est une pièce d'identité.
     * Deux méthodes de détection (ordre de préférence) :
     *  1. registry name == "dlclient:id_card"  (custom Forge item)
     *  2. PDC tag Bukkit "dlcitizens:id_serial"  (PAPER legacy)
     */
    private boolean isIdCardNms(Object nmsStack) {
        if (nmsStack == null) return false;
        try {
            // 1. Vérification par registry name (custom item)
            Object nmsItem  = nmsStack.getClass().getMethod("getItem").invoke(nmsStack);
            Object regName  = nmsItem.getClass().getMethod("getRegistryName").invoke(nmsItem);
            if (regName != null) {
                String ns   = (String) regName.getClass().getMethod("getNamespace").invoke(regName);
                String path = (String) regName.getClass().getMethod("getPath").invoke(regName);
                if ("dlclient".equals(ns) && "id_card".equals(path)) return true;
            }
        } catch (Exception ignored) {}

        try {
            // 2. Vérification par PDC tag (legacy PAPER + PublicBukkitValues)
            Object nbt = nmsStack.getClass().getMethod("getTag").invoke(nmsStack);
            if (nbt == null) return false;
            if (!containsNbt(nbt, "PublicBukkitValues", 10)) return false;
            Object pbv = nbt.getClass().getMethod("getCompound", String.class)
                            .invoke(nbt, "PublicBukkitValues");
            return containsNbt(pbv, "dlcitizens:id_serial", 8);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** setCanceled(true) via réflexion. */
    private void cancelEvent(Object event) {
        try {
            event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
        } catch (Exception e) {
            log.warning("[IdCard] cancelEvent: " + e);
        }
    }

    /**
     * Tente de positionner le résultat d'annulation à FAIL.
     * Empêche le client de simuler localement la consommation de l'item.
     */
    private void trySetCancellationResultFail(Object event) {
        try {
            Class<?> artClass   = Class.forName("net.minecraft.util.ActionResultType");
            Object   failResult = artClass.getField("FAIL").get(null);
            event.getClass().getMethod("setCancellationResult", artClass).invoke(event, failResult);
        } catch (Exception ignored) {}
    }

    /**
     * Planifie un updateInventory() sur le thread Bukkit pour forcer le renvoi
     * de l'inventaire au client et annuler toute prédiction locale de consommation.
     */
    private void scheduleInventoryUpdate(Object nmsPlayer) {
        if (nmsPlayer == null) return;
        Player player = resolvePlayer(nmsPlayer);
        if (player == null) return;
        plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
    }

    /** Résout un NMS PlayerEntity en Player Bukkit (même logique que PacketChannel). */
    private Player resolvePlayer(Object nmsPlayer) {
        if (nmsPlayer instanceof Player) return (Player) nmsPlayer;
        try {
            Object bukkit = nmsPlayer.getClass().getMethod("getBukkitEntity").invoke(nmsPlayer);
            if (bukkit instanceof Player) return (Player) bukkit;
        } catch (Exception ignored) {}
        try {
            Object uuidStr = nmsPlayer.getClass().getMethod("getStringUUID").invoke(nmsPlayer);
            if (uuidStr instanceof String) {
                return plugin.getServer().getPlayer(java.util.UUID.fromString((String) uuidStr));
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Enregistre un Consumer<Event> sur l'EVENT_BUS Forge via la surcharge à 4 paramètres. */
    private void registerForgeEvent(Object eventBus, Object priority,
                                    String eventClassName,
                                    Consumer<Object> handler) {
        try {
            Class<?> eventClass = Class.forName(eventClassName);
            Method   addListener = null;
            for (Method m : eventBus.getClass().getMethods()) {
                if ("addListener".equals(m.getName())
                        && m.getParameterCount() == 4
                        && m.getParameterTypes()[2] == Class.class) {
                    addListener = m;
                    break;
                }
            }
            if (addListener == null) {
                log.warning("[IdCard] addListener introuvable pour " + eventClassName);
                return;
            }
            addListener.invoke(eventBus, priority, false, eventClass, handler);
        } catch (ClassNotFoundException e) {
            log.warning("[IdCard] Classe Forge introuvable: " + eventClassName + " — " + e.getMessage());
        } catch (Exception e) {
            log.warning("[IdCard] registerForgeEvent(" + eventClassName + "): " + e);
        }
    }

    /** Teste contains(String,int) puis hasKey(String,int) selon les mappings runtime. */
    private static boolean containsNbt(Object nbt, String key, int type) throws Exception {
        try {
            return (Boolean) nbt.getClass().getMethod("contains", String.class, int.class)
                                           .invoke(nbt, key, type);
        } catch (NoSuchMethodException e) {
            return (Boolean) nbt.getClass().getMethod("hasKey", String.class, int.class)
                                           .invoke(nbt, key, type);
        }
    }
}
