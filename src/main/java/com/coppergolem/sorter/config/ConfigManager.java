package com.coppergolem.sorter.config;

import com.coppergolem.sorter.CopperGolemSorterPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final CopperGolemSorterPlugin plugin;

    private int itemsPerOperation;
    private int ticksBetweenTransfers;
    private double movementSpeed;
    private int maxStacks;
    private int maxTotalItems;
    private int copperChestDetection;
    private int normalChestDetection;
    private boolean sortingEnabled;
    private boolean preventOverfill;
    private boolean stackItems;
    private List<Material> priorityItems;
    private int maxActiveGolems;
    private boolean debug;

    public ConfigManager(CopperGolemSorterPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        itemsPerOperation = config.getInt("transfer.items-per-operation", 4);
        ticksBetweenTransfers = config.getInt("transfer.ticks-between-transfers", 20);
        movementSpeed = config.getDouble("transfer.movement-speed", 1.2);

        maxStacks = config.getInt("capacity.max-stacks", 9);
        maxTotalItems = config.getInt("capacity.max-total-items", 576);

        copperChestDetection = config.getInt("range.copper-chest-detection", 16);
        normalChestDetection = config.getInt("range.normal-chest-detection", 24);

        sortingEnabled = config.getBoolean("sorting.enabled", true);
        preventOverfill = config.getBoolean("sorting.prevent-overfill", true);
        stackItems = config.getBoolean("sorting.stack-items", true);

        priorityItems = new ArrayList<>();
        List<String> priorityItemStrings = config.getStringList("priority-items");
        for (String itemName : priorityItemStrings) {
            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                priorityItems.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in priority-items: " + itemName);
            }
        }

        maxActiveGolems = config.getInt("performance.max-active-golems", 50);
        debug = config.getBoolean("performance.debug", false);

        if (debug) {
            plugin.getLogger().info("Configuration loaded:");
            plugin.getLogger().info("  Items per operation: " + itemsPerOperation);
            plugin.getLogger().info("  Ticks between transfers: " + ticksBetweenTransfers);
            plugin.getLogger().info("  Movement speed: " + movementSpeed);
            plugin.getLogger().info("  Max stacks: " + maxStacks);
            plugin.getLogger().info("  Priority items: " + priorityItems.size());
        }
    }

    public int getItemsPerOperation() {
        return itemsPerOperation;
    }

    public int getTicksBetweenTransfers() {
        return ticksBetweenTransfers;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public int getMaxStacks() {
        return maxStacks;
    }

    public int getMaxTotalItems() {
        return maxTotalItems;
    }

    public int getCopperChestDetection() {
        return copperChestDetection;
    }

    public int getNormalChestDetection() {
        return normalChestDetection;
    }

    public boolean isSortingEnabled() {
        return sortingEnabled;
    }

    public boolean isPreventOverfill() {
        return preventOverfill;
    }

    public boolean isStackItems() {
        return stackItems;
    }

    public List<Material> getPriorityItems() {
        return priorityItems;
    }

    public int getMaxActiveGolems() {
        return maxActiveGolems;
    }

    public boolean isDebug() {
        return debug;
    }
}
