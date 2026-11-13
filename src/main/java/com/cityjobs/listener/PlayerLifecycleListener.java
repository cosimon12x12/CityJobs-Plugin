package com.cityjobs.listener;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.PlayerJobData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerLifecycleListener implements Listener {

    private final CityJobsPlugin plugin;

    public PlayerLifecycleListener(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // By design, we do absolutely nothing to the player's job data here.
        // The PlayerJobData remains perfectly intact in the JobManager's activeData map.
        // This guarantees the player never loses XP, levels, stats, or their active job upon death.
        // The 10-minute cooldown timestamp also remains untouched.
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfigManager().isTeleportToJobSpawnOnDeath()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerJobData data = plugin.getJobManager().getJobData(player.getUniqueId());

        if (data != null && data.hasJob()) {
            Location jobSpawn = plugin.getSpawnManager().getSpawn(data.getJobType());
            if (jobSpawn != null) {
                event.setRespawnLocation(jobSpawn);
            }
        }
    }
}