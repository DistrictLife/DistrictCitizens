package dev.districtlife.citizens;

import dev.districtlife.citizens.commands.DLCitizenCommand;
import dev.districtlife.citizens.config.PluginConfig;
import dev.districtlife.citizens.creation.CreationSessionManager;
import dev.districtlife.citizens.database.DatabaseManager;
import dev.districtlife.citizens.listeners.*;
import dev.districtlife.citizens.network.PacketChannel;
import dev.districtlife.citizens.network.RateLimiter;
import org.bukkit.plugin.java.JavaPlugin;

public class DLCitizensPlugin extends JavaPlugin {

    private static DLCitizensPlugin instance;
    private DatabaseManager databaseManager;
    private PluginConfig pluginConfig;
    private CreationSessionManager sessionManager;
    private RateLimiter rateLimiter;
    private PacketChannel packetChannel;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginConfig = new PluginConfig(getConfig());

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        sessionManager = new CreationSessionManager(this);
        rateLimiter = new RateLimiter(pluginConfig);
        packetChannel = new PacketChannel(this);

        registerListeners();
        registerCommands();
        registerForgeItemTossProtection();

        getLogger().info("DLCitizens activé.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("DLCitizens désactivé.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new AuthMeRegisterListener(this), this);
        getServer().getPluginManager().registerEvents(new AuthMeLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandPreprocessListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ModCheckListener(this), this);
        getServer().getPluginManager().registerEvents(new IdCardDebugListener(this), this);
    }

    /**
     * Enregistre un listener Forge sur MinecraftForge.EVENT_BUS qui annule le jet
     * (touche Q / throw) d'une pièce d'identité.
     *
     * Sur Arclight, les mods Forge ET les plugins Bukkit partagent le même classloader
     * des classes jeu → Class.forName() donne accès aux classes Forge.
     * On utilise la réflexion pour éviter toute dépendance de compilation vers les classes
     * d'événements Forge non présentes dans notre forge-stub minimal.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerForgeItemTossProtection() {
        try {
            // MinecraftForge.EVENT_BUS
            Class<?> mcForge   = Class.forName("net.minecraftforge.common.MinecraftForge");
            Object   eventBus  = mcForge.getField("EVENT_BUS").get(null);

            // Classe de l'événement
            Class<?> itemTossClass = Class.forName("net.minecraftforge.event.entity.player.ItemTossEvent");

            // EventPriority.HIGHEST pour intercepter avant d'autres mods
            Class<?> priorityClass = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
            Object   highPriority  = priorityClass.getField("HIGHEST").get(null);

            // Consumer<ItemTossEvent> — via lambda (type erasure → Consumer brut)
            java.util.function.Consumer<Object> listener = event -> {
                try {
                    // event.getEntityItem() → EntityItem (net.minecraft.entity.item.ItemEntity)
                    Object entityItem = event.getClass().getMethod("getEntityItem").invoke(event);
                    // entityItem.getItem() → net.minecraft.item.ItemStack (NMS)
                    Object nmsStack = entityItem.getClass().getMethod("getItem").invoke(entityItem);
                    // nmsStack.getTag() → CompoundNBT (ou null)
                    Object nbt = nmsStack.getClass().getMethod("getTag").invoke(nmsStack);
                    if (nbt == null) return;

                    // Vérifie PublicBukkitValues (compound type = 10)
                    boolean hasPBV = containsNbt(nbt, "PublicBukkitValues", 10);
                    if (!hasPBV) return;

                    Object pbv = nbt.getClass().getMethod("getCompound", String.class)
                                              .invoke(nbt, "PublicBukkitValues");
                    // Vérifie dlcitizens:id_serial (string type = 8)
                    boolean hasSerial = containsNbt(pbv, "dlcitizens:id_serial", 8);
                    if (!hasSerial) return;

                    // Annule le jet
                    event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
                    getLogger().fine("[IdCard] ItemTossEvent annulé — carte d'identité protégée.");
                } catch (Exception ex) {
                    getLogger().warning("[IdCard] Erreur handler ItemTossEvent: " + ex);
                }
            };

            // Cherche addListener(priority, receiveCancelled, Class<T>, Consumer<T>)
            java.lang.reflect.Method addListener = null;
            for (java.lang.reflect.Method m : eventBus.getClass().getMethods()) {
                if ("addListener".equals(m.getName()) && m.getParameterCount() == 4
                        && m.getParameterTypes()[2] == Class.class) {
                    addListener = m;
                    break;
                }
            }

            if (addListener != null) {
                addListener.invoke(eventBus, highPriority, false, itemTossClass, listener);
                getLogger().info("[IdCard] Protection Forge ItemTossEvent enregistrée.");
            } else {
                getLogger().warning("[IdCard] addListener(priority,bool,class,consumer) introuvable sur l'EventBus.");
            }
        } catch (Exception e) {
            getLogger().warning("[IdCard] Impossible d'enregistrer la protection Forge ItemTossEvent: " + e.getMessage());
        }
    }

    /** Appelle contains(String, int) ou hasKey(String, int) selon la version de mappings. */
    private static boolean containsNbt(Object nbt, String key, int type) throws Exception {
        try {
            return (Boolean) nbt.getClass().getMethod("contains", String.class, int.class)
                                           .invoke(nbt, key, type);
        } catch (NoSuchMethodException e) {
            return (Boolean) nbt.getClass().getMethod("hasKey", String.class, int.class)
                                           .invoke(nbt, key, type);
        }
    }

    private void registerCommands() {
        DLCitizenCommand cmd = new DLCitizenCommand(this);
        getCommand("dlcitizen").setExecutor(cmd);
        getCommand("dlcitizen").setTabCompleter(cmd);
    }

    public static DLCitizensPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public CreationSessionManager getSessionManager() {
        return sessionManager;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public PacketChannel getPacketChannel() {
        return packetChannel;
    }
}
