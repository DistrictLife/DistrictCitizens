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
