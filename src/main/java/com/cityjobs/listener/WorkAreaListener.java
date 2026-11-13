package com.cityjobs.listener;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import com.cityjobs.model.PlayerJobData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class WorkAreaListener implements Listener {

    private final CityJobsPlugin plugin;

    public WorkAreaListener(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        Material material = block.getType();

        // 1. Identify if this block belongs to ANY job
        JobType owningJob = plugin.getConfigManager().findOwningJob(material);
        if (owningJob == null) {
            return; // Not a job resource, ignore it completely
        }

        // 2. Fetch Player Data
        PlayerJobData data = plugin.getJobManager().getJobData(player.getUniqueId());
        
        // 3. Verify Job Match
        if (data == null || !data.hasJob() || data.getJobType() != owningJob) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("wrong-job-resource", "%required_job%", plugin.getConfigManager().getJobDisplayName(owningJob)));
            return;
        }

        // 4. Verify Location (Work Area)
        if (!plugin.getAreaManager().isInWorkArea(owningJob, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("outside-work-area", "%job%", plugin.getConfigManager().getJobDisplayName(owningJob)));
            return;
        }

        // 5. Special Farmer check: only reward fully grown crops
        if (owningJob == JobType.FARMER && block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() != ageable.getMaximumAge()) {
                return; // Allow breaking, but don't reward XP for unripe crops
            }
        }

        // 6. Reward Player
        data.addResourceCollected();
        data.addXp(plugin.getConfigManager().getExpPerResource());
        checkLevelUp(player, data);
        
        // Note: The actual items drop naturally. We do NOT auto-sell here.
        // Selling happens safely through /sell commands in Phase 4.
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Optional: Prevent players from placing job blocks inside the work area to farm XP infinitely.
        // For standard production servers, preventing placement of ores in the miner region is crucial.
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Material material = event.getBlockPlaced().getType();
        JobType owningJob = plugin.getConfigManager().findOwningJob(material);
        
        if (owningJob == JobType.MINER || owningJob == JobType.LUMBERJACK) {
            if (plugin.getAreaManager().isInWorkArea(owningJob, event.getBlockPlaced().getLocation())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().colorize("&cYou cannot place job resources inside the work area."));
            }
        }
    }

    private void checkLevelUp(Player player, PlayerJobData data) {
        double currentXp = data.getXp();
        int currentLevel = data.getLevel();
        // Simple formula: required = base * level
        double requiredXp = plugin.getConfigManager().getBaseLevelUpExp() * currentLevel;

        if (currentXp >= requiredXp) {
            data.setLevel(currentLevel + 1);
            data.setXp(currentXp - requiredXp);
            player.sendMessage(plugin.getConfigManager().colorize("&8[&6CityJobs&8] &aLevel Up! You are now level &e" + data.getLevel() + " &ain " + plugin.getConfigManager().getJobDisplayName(data.getJobType()) + "!"));
        }
    }
}