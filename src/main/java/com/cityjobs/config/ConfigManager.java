package com.cityjobs.config;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("0.00");

    private final CityJobsPlugin plugin;
    private final Map<JobType, Map<Material, Double>> jobPrices = new EnumMap<>(JobType.class);

    private String currencySymbol = "$";
    private long minimumJobDurationSeconds = 600L;
    private int sellCooldownSeconds = 2;
    private boolean requireJobToSell = true;
    private boolean restrictOutsideArea = true;
    private boolean teleportToJobSpawnOnDeath = true;
    private double expPerResource = 5.0;
    private double baseLevelUpExp = 100.0;

    public ConfigManager(CityJobsPlugin plugin) {
        this.plugin = plugin;
        for (JobType type : JobType.values()) {
            jobPrices.put(type, new LinkedHashMap<>());
        }
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.currencySymbol = config.getString("economy.currency-symbol", "$");
        this.minimumJobDurationSeconds = Math.max(0L, config.getLong("settings.minimum-job-duration", 600L));
        this.sellCooldownSeconds = Math.max(0, config.getInt("settings.sell-cooldown", 2));
        this.requireJobToSell = config.getBoolean("settings.require-job-to-sell", true);
        this.restrictOutsideArea = config.getBoolean("settings.restrict-outside-area", true);
        this.teleportToJobSpawnOnDeath = config.getBoolean("settings.teleport-to-job-spawn-on-death", true);
        this.expPerResource = Math.max(0.1, config.getDouble("settings.exp-per-resource", 5.0));
        this.baseLevelUpExp = Math.max(10.0, config.getDouble("settings.base-level-up-exp", 100.0));

        loadAllJobPrices(config);
    }

    public void reload() {
        load();
    }

    private void loadAllJobPrices(FileConfiguration config) {
        for (JobType jobType : JobType.values()) {
            Map<Material, Double> priceMap = new LinkedHashMap<>();

            // Primary dynamic section: jobs.<job>.prices
            String primaryPath = "jobs." + jobType.getConfigKey() + ".prices";
            loadPriceSection(config.getConfigurationSection(primaryPath), priceMap, jobType);

            // Legacy fallback section: economy.prices.<wood|minerals|farming>
            String legacyCategory = switch (jobType) {
                case LUMBERJACK -> "wood";
                case MINER -> "minerals";
                case FARMER -> "farming";
            };
            String fallbackPath = "economy.prices." + legacyCategory;
            loadPriceSection(config.getConfigurationSection(fallbackPath), priceMap, jobType);

            jobPrices.put(jobType, priceMap);
            plugin.getLogger().info("Loaded " + priceMap.size() + " configurable materials for job " + jobType.getDefaultDisplayName() + ".");
        }
    }

    private void loadPriceSection(ConfigurationSection section, Map<Material, Double> target, JobType jobType) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key.trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                plugin.getLogger().warning("Skipping unknown material '" + key + "' in configuration for job " + jobType.getConfigKey() + ".");
                continue;
            }
            double price = section.getDouble(key, -1.0);
            if (price < 0.0 || Double.isNaN(price) || Double.isInfinite(price)) {
                plugin.getLogger().warning("Skipping invalid price for material '" + key + "' in job " + jobType.getConfigKey() + ".");
                continue;
            }
            target.put(material, price);
        }
    }

    public Map<Material, Double> getJobPrices(JobType jobType) {
        Map<Material, Double> map = jobPrices.get(jobType);
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    public double getMaterialPrice(JobType jobType, Material material) {
        if (jobType == null || material == null) {
            return -1.0;
        }
        Map<Material, Double> map = jobPrices.get(jobType);
        if (map == null) {
            return -1.0;
        }
        return map.getOrDefault(material, -1.0);
    }

    public boolean isJobMaterial(JobType jobType, Material material) {
        return getMaterialPrice(jobType, material) >= 0.0;
    }

    public JobType findOwningJob(Material material) {
        if (material == null) {
            return null;
        }
        for (JobType type : JobType.values()) {
            if (isJobMaterial(type, material)) {
                return type;
            }
        }
        return null;
    }

    public void setMaterialPrice(JobType jobType, Material material, double price) {
        Map<Material, Double> map = jobPrices.computeIfAbsent(jobType, k -> new LinkedHashMap<>());
        map.put(material, price);

        String path = "jobs." + jobType.getConfigKey() + ".prices." + material.name();
        plugin.getConfig().set(path, price);
        plugin.saveConfig();
    }

    public void saveJobSpawn(JobType jobType, Location location) {
        String base = "spawns." + jobType.getConfigKey();
        FileConfiguration config = plugin.getConfig();
        config.set(base + ".enabled", true);
        config.set(base + ".world", location.getWorld().getName());
        config.set(base + ".x", location.getX());
        config.set(base + ".y", location.getY());
        config.set(base + ".z", location.getZ());
        config.set(base + ".yaw", (double) location.getYaw());
        config.set(base + ".pitch", (double) location.getPitch());
        plugin.saveConfig();
    }

    public Location getJobSpawn(JobType jobType) {
        if (jobType == null) {
            return null;
        }
        String base = "spawns." + jobType.getConfigKey();
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean(base + ".enabled", false)) {
            return null;
        }
        String worldName = config.getString(base + ".world");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = config.getDouble(base + ".x");
        double y = config.getDouble(base + ".y");
        double z = config.getDouble(base + ".z");
        float yaw = (float) config.getDouble(base + ".yaw");
        float pitch = (float) config.getDouble(base + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void saveWorkArea(JobType jobType, String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        String base = "areas." + jobType.getConfigKey();
        FileConfiguration config = plugin.getConfig();
        config.set(base + ".enabled", true);
        config.set(base + ".world", worldName);
        config.set(base + ".x1", x1);
        config.set(base + ".y1", y1);
        config.set(base + ".z1", z1);
        config.set(base + ".x2", x2);
        config.set(base + ".y2", y2);
        config.set(base + ".z2", z2);
        plugin.saveConfig();
    }

    public String getJobDisplayName(JobType jobType) {
        return plugin.getConfig().getString("jobs." + jobType.getConfigKey() + ".display-name", jobType.getDefaultDisplayName());
    }

    public String getJobDescription(JobType jobType) {
        return plugin.getConfig().getString("jobs." + jobType.getConfigKey() + ".description", "");
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public String formatMoney(double amount) {
        return MONEY_FORMAT.format(amount);
    }

    public long getMinimumJobDurationSeconds() {
        return minimumJobDurationSeconds;
    }

    public int getSellCooldownSeconds() {
        return sellCooldownSeconds;
    }

    public boolean isRequireJobToSell() {
        return requireJobToSell;
    }

    public boolean isRestrictOutsideArea() {
        return restrictOutsideArea;
    }

    public boolean isTeleportToJobSpawnOnDeath() {
        return teleportToJobSpawnOnDeath;
    }

    public double getExpPerResource() {
        return expPerResource;
    }

    public double getBaseLevelUpExp() {
        return baseLevelUpExp;
    }

    public String getRawMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "&cMissing message: " + key);
    }

    public String getMessage(String key, String... replacements) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6CityJobs&8] &r");
        String message = prefix + getRawMessage(key);
        if (replacements != null && replacements.length >= 2) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return colorize(message);
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}