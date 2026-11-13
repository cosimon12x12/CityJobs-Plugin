package com.cityjobs.manager;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import org.bukkit.Location;

import java.util.EnumMap;
import java.util.Map;

public class SpawnManager {

    private final CityJobsPlugin plugin;
    private final Map<JobType, Location> jobSpawns = new EnumMap<>(JobType.class);

    public SpawnManager(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadSpawns() {
        jobSpawns.clear();
        for (JobType type : JobType.values()) {
            Location loc = plugin.getConfigManager().getJobSpawn(type);
            if (loc != null) {
                jobSpawns.put(type, loc);
            }
        }
        plugin.getLogger().info("Loaded " + jobSpawns.size() + " active job spawns.");
    }

    public void setSpawn(JobType type, Location location) {
        plugin.getConfigManager().saveJobSpawn(type, location);
        jobSpawns.put(type, location);
    }

    public Location getSpawn(JobType type) {
        return jobSpawns.get(type);
    }
}