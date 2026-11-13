package com.cityjobs.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public enum JobType {
    LUMBERJACK("lumberjack", "Lumberjack", "sellwood", Material.IRON_AXE),
    MINER("miner", "Miner", "sellmines", Material.IRON_PICKAXE),
    FARMER("farmer", "Farmer", "sellfarm", Material.IRON_HOE);

    private final String configKey;
    private final String defaultDisplayName;
    private final String sellCommand;
    private final Material icon;

    JobType(String configKey, String defaultDisplayName, String sellCommand, Material icon) {
        this.configKey = configKey;
        this.defaultDisplayName = defaultDisplayName;
        this.sellCommand = sellCommand;
        this.icon = icon;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public String getSellCommand() {
        return sellCommand;
    }

    public Material getIcon() {
        return icon;
    }

    public static Optional<JobType> fromString(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        for (JobType type : values()) {
            if (type.name().equals(normalized) || type.configKey.equalsIgnoreCase(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}