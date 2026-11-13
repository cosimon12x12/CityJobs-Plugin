package com.cityjobs.command;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {

    private final CityJobsPlugin plugin;

    public SellCommand(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("cityjobs.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        
        JobType targetJob = null;
        for (JobType type : JobType.values()) {
            if (type.getSellCommand().equals(cmdName)) {
                targetJob = type;
                break;
            }
        }

        if (targetJob != null) {
            plugin.getEconomyManager().processSale(player, targetJob);
        } else {
            player.sendMessage(plugin.getConfigManager().colorize("&cAn error occurred: Unmapped sell command."));
        }

        return true;
    }
}