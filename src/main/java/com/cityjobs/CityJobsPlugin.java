package com.cityjobs;

import com.cityjobs.command.JobCommand;
import com.cityjobs.command.JobsAdminCommand;
import com.cityjobs.command.SellCommand;
import com.cityjobs.config.ConfigManager;
import com.cityjobs.database.DatabaseManager;
import com.cityjobs.gui.JobMenuGui;
import com.cityjobs.listener.PlayerLifecycleListener;
import com.cityjobs.listener.WorkAreaListener;
import com.cityjobs.manager.AreaManager;
import com.cityjobs.manager.EconomyManager;
import com.cityjobs.manager.JobManager;
import com.cityjobs.manager.SpawnManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CityJobsPlugin extends JavaPlugin implements Listener {

    private static CityJobsPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private JobManager jobManager;
    private AreaManager areaManager;
    private EconomyManager economyManager;
    private SpawnManager spawnManager;
    private JobMenuGui jobMenuGui;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // Phase 1: Configuration
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        if (!verifySqliteDriver()) {
            getLogger().severe("SQLite JDBC driver not found! Disabling CityJobs.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Phase 2: Database and Basic Managers
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init();
        
        this.jobManager = new JobManager(this);
        this.jobMenuGui = new JobMenuGui(this);

        // Phase 3: Area Manager
        this.areaManager = new AreaManager(this);
        this.areaManager.loadAreas();

        // Phase 4: Economy Manager
        this.economyManager = new EconomyManager(this);

        // Phase 5: Spawn Manager
        this.spawnManager = new SpawnManager(this);
        this.spawnManager.loadSpawns();

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(this.jobManager, this);
        Bukkit.getPluginManager().registerEvents(this.jobMenuGui, this);
        Bukkit.getPluginManager().registerEvents(new WorkAreaListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this), this);

        // Register Commands
        JobCommand jobCommand = new JobCommand(this);
        if (getCommand("job") != null) {
            getCommand("job").setExecutor(jobCommand);
            getCommand("job").setTabCompleter(jobCommand);
        }

        SellCommand sellCommand = new SellCommand(this);
        if (getCommand("sellwood") != null) getCommand("sellwood").setExecutor(sellCommand);
        if (getCommand("sellmines") != null) getCommand("sellmines").setExecutor(sellCommand);
        if (getCommand("sellfarm") != null) getCommand("sellfarm").setExecutor(sellCommand);
        
        JobsAdminCommand adminCommand = new JobsAdminCommand(this);
        if (getCommand("jobsadmin") != null) {
            getCommand("jobsadmin").setExecutor(adminCommand);
            getCommand("jobsadmin").setTabCompleter(adminCommand);
        }

        // Vault Economy Hook
        setupEconomy();

        // Safe reload data for online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                databaseManager.loadJobDataSync(p.getUniqueId());
            });
        }

        getLogger().info("CityJobs Phase 6 initialized successfully for Paper 1.21.11.");
    }

    @Override
    public void onDisable() {
        if (this.jobManager != null) {
            this.jobManager.saveAllSync(); 
        }
        getLogger().info("CityJobs has been disabled cleanly.");
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (this.economy == null) {
            if (setupEconomy()) {
                getLogger().info("Successfully hooked into Vault Economy provider after server load: " + economy.getName());
            } else {
                getLogger().warning("No Vault Economy provider was found! Selling features will remain unavailable until an economy plugin is active.");
            }
        }
    }

    private boolean verifySqliteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException ex) {
            getLogger().severe("Failed to load org.sqlite.JDBC: " + ex.getMessage());
            return false;
        }
    }

    public boolean setupEconomy() {
        Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        if (this.economy != null) {
            getLogger().info("Hooked into Vault Economy provider: " + this.economy.getName());
            return true;
        }
        return false;
    }

    public static CityJobsPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public JobManager getJobManager() { return jobManager; }
    public AreaManager getAreaManager() { return areaManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public JobMenuGui getJobMenuGui() { return jobMenuGui; }
    public Economy getEconomy() { return economy; }
    public boolean hasEconomy() {
        if (economy == null) setupEconomy();
        return economy != null;
    }
}