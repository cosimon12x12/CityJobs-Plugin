package com.cityjobs.manager;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import com.cityjobs.model.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager implements Listener {

    private final CityJobsPlugin plugin;
    private final ConcurrentHashMap<UUID, PlayerJobData> activeData = new ConcurrentHashMap<>();

    public JobManager(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerJobData data = plugin.getDatabaseManager().loadJobDataSync(uuid);
            activeData.put(uuid, data);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerJobData data = activeData.remove(uuid);
        if (data != null) {
            plugin.getDatabaseManager().saveJobDataAsync(data);
        }
    }

    public PlayerJobData getJobData(UUID uuid) {
        return activeData.get(uuid);
    }

    public void handleJoinJob(Player player, JobType jobType) {
        PlayerJobData data = activeData.get(player.getUniqueId());
        if (data == null) return;

        if (data.hasJob()) {
            if (data.getJobType() == jobType) {
                player.sendMessage(plugin.getConfigManager().getMessage("job-already-in", "%job%", plugin.getConfigManager().getJobDisplayName(jobType)));
                return;
            }
            player.sendMessage(plugin.getConfigManager().getMessage("job-already-has-other", "%job%", plugin.getConfigManager().getJobDisplayName(data.getJobType())));
            return;
        }

        data.setJobType(jobType);
        data.setJoinTimestamp(System.currentTimeMillis());
        plugin.getDatabaseManager().saveJobDataAsync(data);
        
        player.sendMessage(plugin.getConfigManager().getMessage("job-joined", "%job%", plugin.getConfigManager().getJobDisplayName(jobType)));
    }

    public void handleQuitJob(Player player) {
        PlayerJobData data = activeData.get(player.getUniqueId());
        if (data == null) return;

        if (!data.hasJob()) {
            player.sendMessage(plugin.getConfigManager().getMessage("job-none"));
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - data.getJoinTimestamp();
        long requiredMillis = plugin.getConfigManager().getMinimumJobDurationSeconds() * 1000L;

        if (elapsedMillis < requiredMillis) {
            long remainingSeconds = (requiredMillis - elapsedMillis) / 1000L;
            long mins = remainingSeconds / 60;
            long secs = remainingSeconds % 60;
            String timeFormat = mins + "m " + secs + "s";
            player.sendMessage(plugin.getConfigManager().getMessage("job-quit-cooldown", "%time%", timeFormat));
            return;
        }

        JobType oldJob = data.getJobType();
        data.clearJob();
        plugin.getDatabaseManager().saveJobDataAsync(data);
        
        player.sendMessage(plugin.getConfigManager().getMessage("job-quit-success", "%job%", plugin.getConfigManager().getJobDisplayName(oldJob)));
    }
    
    public void saveAllSync() {
        for (PlayerJobData data : activeData.values()) {
            plugin.getDatabaseManager().saveJobDataSync(data);
        }
    }
}