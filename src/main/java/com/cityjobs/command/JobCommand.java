package com.cityjobs.command;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import com.cityjobs.model.PlayerJobData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JobCommand implements CommandExecutor, TabCompleter {

    private final CityJobsPlugin plugin;

    public JobCommand(CityJobsPlugin plugin) {
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

        if (args.length == 0) {
            plugin.getJobMenuGui().openMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "join":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /job join <lumberjack|miner|farmer>");
                    return true;
                }
                Optional<JobType> jobOpt = JobType.fromString(args[1]);
                if (jobOpt.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Invalid job type. Available: lumberjack, miner, farmer.");
                    return true;
                }
                plugin.getJobManager().handleJoinJob(player, jobOpt.get());
                break;

            case "quit":
                plugin.getJobManager().handleQuitJob(player);
                break;

            case "info":
            case "stats":
                displayStats(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Use /job [join|quit|info|stats]");
                break;
        }
        return true;
    }

    private void displayStats(Player player) {
        PlayerJobData data = plugin.getJobManager().getJobData(player.getUniqueId());
        if (data == null || !data.hasJob()) {
            player.sendMessage(plugin.getConfigManager().getMessage("job-none"));
            return;
        }

        String displayName = plugin.getConfigManager().getJobDisplayName(data.getJobType());
        player.sendMessage(ChatColor.DARK_GRAY + "--- " + ChatColor.GOLD + "Job Statistics: " + displayName + ChatColor.DARK_GRAY + " ---");
        player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + data.getLevel());
        player.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE + String.format("%.2f", data.getXp()));
        player.sendMessage(ChatColor.YELLOW + "Resources Gathered: " + ChatColor.WHITE + data.getResourcesCollected());
        player.sendMessage(ChatColor.YELLOW + "Total Earned: " + ChatColor.GREEN + plugin.getConfigManager().getCurrencySymbol() + plugin.getConfigManager().formatMoney(data.getMoneyEarned()));
        player.sendMessage(ChatColor.DARK_GRAY + "-------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player) || !sender.hasPermission("cityjobs.use")) return completions;

        if (args.length == 1) {
            List<String> options = Arrays.asList("join", "quit", "info", "stats");
            for (String opt : options) {
                if (opt.startsWith(args[0].toLowerCase())) completions.add(opt);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            List<String> jobs = Arrays.asList("lumberjack", "miner", "farmer");
            for (String job : jobs) {
                if (job.startsWith(args[1].toLowerCase())) completions.add(job);
            }
        }
        return completions;
    }
}