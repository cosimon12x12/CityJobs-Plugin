package com.cityjobs.gui;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class JobMenuGui implements Listener {

    private final CityJobsPlugin plugin;

    public JobMenuGui(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        JobMenuHolder holder = new JobMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Available City Jobs");
        holder.setInventory(inv);

        inv.setItem(11, createJobIcon(JobType.LUMBERJACK));
        inv.setItem(13, createJobIcon(JobType.MINER));
        inv.setItem(15, createJobIcon(JobType.FARMER));

        player.openInventory(inv);
    }

    private ItemStack createJobIcon(JobType jobType) {
        ItemStack item = new ItemStack(jobType.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + plugin.getConfigManager().getJobDisplayName(jobType));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + plugin.getConfigManager().getJobDescription(jobType));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to join this job!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof JobMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        JobType selected = null;
        if (clickedItem.getType() == JobType.LUMBERJACK.getIcon()) selected = JobType.LUMBERJACK;
        else if (clickedItem.getType() == JobType.MINER.getIcon()) selected = JobType.MINER;
        else if (clickedItem.getType() == JobType.FARMER.getIcon()) selected = JobType.FARMER;

        if (selected != null) {
            player.closeInventory();
            plugin.getJobManager().handleJoinJob(player, selected);
        }
    }
}