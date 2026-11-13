package com.cityjobs.manager;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import com.cityjobs.model.PlayerJobData;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    private final CityJobsPlugin plugin;
    private final ConcurrentHashMap<UUID, Long> sellCooldowns = new ConcurrentHashMap<>();

    public EconomyManager(CityJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void processSale(Player player, JobType requiredJob) {
        // 1. Verify Economy Availability
        if (!plugin.hasEconomy()) {
            player.sendMessage(plugin.getConfigManager().colorize("&cThe server economy system is currently unavailable."));
            return;
        }

        // 2. Verify Job Requirements
        PlayerJobData data = plugin.getJobManager().getJobData(player.getUniqueId());
        if (plugin.getConfigManager().isRequireJobToSell()) {
            if (data == null || !data.hasJob() || data.getJobType() != requiredJob) {
                player.sendMessage(plugin.getConfigManager().getMessage("sell-wrong-job", "%job%", plugin.getConfigManager().getJobDisplayName(requiredJob)));
                return;
            }
        }

        // 3. Verify Cooldowns
        long now = System.currentTimeMillis();
        long lastSell = sellCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMillis = plugin.getConfigManager().getSellCooldownSeconds() * 1000L;
        if (now - lastSell < cooldownMillis) {
            long remaining = (cooldownMillis - (now - lastSell)) / 1000L;
            player.sendMessage(plugin.getConfigManager().getMessage("sell-cooldown", "%seconds%", String.valueOf(remaining)));
            return;
        }

        // 4. Calculate Total Value
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        
        double totalValue = 0.0;
        int totalItems = 0;

        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) continue;
            
            double price = plugin.getConfigManager().getMaterialPrice(requiredJob, item.getType());
            if (price > 0.0) {
                totalValue += (price * item.getAmount());
                totalItems += item.getAmount();
            }
        }

        if (totalItems == 0 || totalValue <= 0.0) {
            player.sendMessage(plugin.getConfigManager().getMessage("sell-empty", "%job%", plugin.getConfigManager().getJobDisplayName(requiredJob)));
            return;
        }

        // 5. Execute Vault Transaction (Safe Deposit First)
        EconomyResponse response = plugin.getEconomy().depositPlayer(player, totalValue);
        
        if (response.transactionSuccess()) {
            // 6. Transaction Succeeded -> Remove Items
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType().isAir()) continue;
                
                double price = plugin.getConfigManager().getMaterialPrice(requiredJob, item.getType());
                if (price > 0.0) {
                    inventory.setItem(i, null);
                }
            }

            // 7. Update Statistics
            if (data != null) {
                data.addMoneyEarned(totalValue);
                // Save asynchronously to prevent lag spikes on mass selling
                plugin.getDatabaseManager().saveJobDataAsync(data);
            }

            // Apply Cooldown and Notify Player
            sellCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            
            String formattedMoney = plugin.getConfigManager().formatMoney(totalValue);
            String currency = plugin.getConfigManager().getCurrencySymbol();
            player.sendMessage(plugin.getConfigManager().getMessage("sell-success", "%amount%", String.valueOf(totalItems), "%currency%", currency, "%total%", formattedMoney));
            
        } else {
            // Transaction Failed -> Items remain untouched
            player.sendMessage(plugin.getConfigManager().getMessage("sell-error"));
            plugin.getLogger().warning("Failed economy deposit for " + player.getName() + ". Vault response: " + response.errorMessage);
        }
    }
}