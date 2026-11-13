package com.cityjobs.command;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import com.cityjobs.model.PlayerJobData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JobsAdminCommand implements CommandExecutor, TabCompleter {

    private final CityJobsPlugin plugin;

    public JobsAdminCommand(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cityjobs.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "setspawn":
                handleSetSpawn(sender, args);
                break;
            case "pos1":
                handlePos1(sender, args);
                break;
            case "pos2":
                handlePos2(sender, args);
                break;
            case "createarea":
                handleCreateArea(sender, args);
                break;
            case "setprice":
                handleSetPrice(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "--- " + ChatColor.GOLD + "CityJobs Admin Menu" + ChatColor.DARK_GRAY + " ---");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin reload " + ChatColor.GRAY + "- Reload config and prices");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin setspawn <job> " + ChatColor.GRAY + "- Set job respawn point");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin pos1 <job> " + ChatColor.GRAY + "- Select region point 1");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin pos2 <job> " + ChatColor.GRAY + "- Select region point 2");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin createarea <job> " + ChatColor.GRAY + "- Save work area");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin setprice <job> <material> <price> " + ChatColor.GRAY + "- Set material price");
        sender.sendMessage(ChatColor.YELLOW + "/jobsadmin info <player> " + ChatColor.GRAY + "- View player stats");
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        plugin.getAreaManager().loadAreas();
        plugin.getSpawnManager().loadSpawns();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobsadmin setspawn <job>");
            return;
        }
        Optional<JobType> jobOpt = JobType.fromString(args[1]);
        if (jobOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Invalid job type.");
            return;
        }
        plugin.getSpawnManager().setSpawn(jobOpt.get(), player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Spawn point for " + jobOpt.get().getDefaultDisplayName() + " has been set to your location.");
    }

    private void handlePos1(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        plugin.getAreaManager().setPos1(player);
        Location loc = player.getLocation();
        player.sendMessage(ChatColor.GREEN + "Position 1 set to: X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
    }

    private void handlePos2(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        plugin.getAreaManager().setPos2(player);
        Location loc = player.getLocation();
        player.sendMessage(ChatColor.GREEN + "Position 2 set to: X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
    }

    private void handleCreateArea(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /jobsadmin createarea <job>");
            return;
        }
        Optional<JobType> jobOpt = JobType.fromString(args[1]);
        if (jobOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Invalid job type.");
            return;
        }
        
        Location pos1 = plugin.getAreaManager().getPos1(player);
        Location pos2 = plugin.getAreaManager().getPos2(player);
        
        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "You must set both pos1 and pos2 first using /jobsadmin pos1|pos2.");
            return;
        }
        if (!pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
            player.sendMessage(ChatColor.RED + "Positions must be in the same world.");
            return;
        }

        JobType job = jobOpt.get();
        plugin.getConfigManager().saveWorkArea(job, pos1.getWorld().getName(),
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
        
        plugin.getAreaManager().loadAreas(); // Refresh active areas
        plugin.getAreaManager().clearSelections(player);
        
        player.sendMessage(ChatColor.GREEN + "Work area for " + job.getDefaultDisplayName() + " has been created successfully!");
    }

    private void handleSetPrice(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /jobsadmin setprice <job> <material> <price>");
            return;
        }
        Optional<JobType> jobOpt = JobType.fromString(args[1]);
        if (jobOpt.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Invalid job type.");
            return;
        }
        
        Material material = Material.matchMaterial(args[2]);
        if (material == null) {
            sender.sendMessage(ChatColor.RED + "Unknown material: " + args[2]);
            return;
        }
        
        double price;
        try {
            price = Double.parseDouble(args[3]);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Price must be a positive number.");
            return;
        }

        JobType job = jobOpt.get();
        plugin.getConfigManager().setMaterialPrice(job, material, price);
        sender.sendMessage(ChatColor.GREEN + "Set price of " + material.name() + " to " + plugin.getConfigManager().formatMoney(price) + " for " + job.getDefaultDisplayName() + ".");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jobsadmin info <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found or is offline.");
            return;
        }

        PlayerJobData data = plugin.getJobManager().getJobData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "Could not retrieve data for " + target.getName() + ".");
            return;
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "--- " + ChatColor.GOLD + "Player Info: " + target.getName() + ChatColor.DARK_GRAY + " ---");
        if (data.hasJob()) {
            sender.sendMessage(ChatColor.YELLOW + "Active Job: " + ChatColor.WHITE + plugin.getConfigManager().getJobDisplayName(data.getJobType()));
            sender.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + data.getLevel());
            sender.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE + String.format("%.2f", data.getXp()));
            sender.sendMessage(ChatColor.YELLOW + "Resources: " + ChatColor.WHITE + data.getResourcesCollected());
            sender.sendMessage(ChatColor.YELLOW + "Money Earned: " + ChatColor.GREEN + plugin.getConfigManager().getCurrencySymbol() + plugin.getConfigManager().formatMoney(data.getMoneyEarned()));
        } else {
            sender.sendMessage(ChatColor.RED + "This player does not currently have an active job.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("cityjobs.admin")) return completions;

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "setspawn", "pos1", "pos2", "createarea", "setprice", "info");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) completions.add(sub);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("setspawn", "pos1", "pos2", "createarea", "setprice").contains(sub)) {
                List<String> jobs = Arrays.asList("lumberjack", "miner", "farmer");
                for (String job : jobs) {
                    if (job.startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add(job);
                }
            } else if (sub.equals("info")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setprice")) {
            // Provide a few common suggestions to make typing easier
            List<String> suggestions = Arrays.asList("OAK_LOG", "DIAMOND_ORE", "WHEAT");
            for (String s : suggestions) {
                if (s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) completions.add(s);
            }
        }

        return completions;
    }
}