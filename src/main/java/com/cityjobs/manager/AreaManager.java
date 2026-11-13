package com.cityjobs.manager;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.CuboidRegion;
import com.cityjobs.model.JobType;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AreaManager {

    private final CityJobsPlugin plugin;
    private final Map<JobType, CuboidRegion> jobRegions = new EnumMap<>(JobType.class);
    
    // Admin Selection Cache
    private final Map<UUID, Location> pos1Cache = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Cache = new ConcurrentHashMap<>();

    public AreaManager(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAreas() {
        jobRegions.clear();
        FileConfiguration config = plugin.getConfig();
        for (JobType type : JobType.values()) {
            String base = "areas." + type.getConfigKey();
            if (config.getBoolean(base + ".enabled", false)) {
                String world = config.getString(base + ".world", "world");
                int x1 = config.getInt(base + ".x1");
                int y1 = config.getInt(base + ".y1");
                int z1 = config.getInt(base + ".z1");
                int x2 = config.getInt(base + ".x2");
                int y2 = config.getInt(base + ".y2");
                int z2 = config.getInt(base + ".z2");
                jobRegions.put(type, new CuboidRegion(world, x1, y1, z1, x2, y2, z2));
            }
        }
        plugin.getLogger().info("Loaded " + jobRegions.size() + " active work areas.");
    }

    public boolean isInWorkArea(JobType jobType, Location location) {
        if (!plugin.getConfigManager().isRestrictOutsideArea()) {
            return true; // Restriction disabled globally
        }
        CuboidRegion region = jobRegions.get(jobType);
        if (region == null) {
            return true; // No region configured yet, allow collection as fallback
        }
        return region.contains(location);
    }

    public void setPos1(Player admin) {
        pos1Cache.put(admin.getUniqueId(), admin.getLocation().getBlock().getLocation());
    }

    public void setPos2(Player admin) {
        pos2Cache.put(admin.getUniqueId(), admin.getLocation().getBlock().getLocation());
    }

    public Location getPos1(Player admin) {
        return pos1Cache.get(admin.getUniqueId());
    }

    public Location getPos2(Player admin) {
        return pos2Cache.get(admin.getUniqueId());
    }

    public void clearSelections(Player admin) {
        pos1Cache.remove(admin.getUniqueId());
        pos2Cache.remove(admin.getUniqueId());
    }

    public void registerNewArea(JobType type, CuboidRegion region) {
        jobRegions.put(type, region);
    }
}