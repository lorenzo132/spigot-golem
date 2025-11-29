package com.coppergolem.sorter.controller;

import com.coppergolem.sorter.CopperGolemSorterPlugin;
import com.coppergolem.sorter.config.ConfigManager;
import com.coppergolem.sorter.model.GolemTask;
import com.coppergolem.sorter.sorting.ItemSorter;
import com.coppergolem.sorter.transfer.ChestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GolemController {

    private final CopperGolemSorterPlugin plugin;
    private final Map<UUID, GolemTask> activeTasks;
    private final ChestManager chestManager;
    private ItemSorter itemSorter;
    private BukkitTask updateTask;
    private EntityType copperGolemType;

    public GolemController(CopperGolemSorterPlugin plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
        this.chestManager = new ChestManager();
        this.itemSorter = createItemSorter();
        this.copperGolemType = resolveCopperGolemType();
    }

    public void start() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 1L);
        plugin.getLogger().info("Golem controller started");
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        activeTasks.clear();
        plugin.getLogger().info("Golem controller stopped");
    }

    public void reload() {
        itemSorter = createItemSorter();
        this.copperGolemType = resolveCopperGolemType();
        plugin.getLogger().info("Golem controller reloaded");
    }

    private ItemSorter createItemSorter() {
        ConfigManager config = plugin.getConfigManager();
        return new ItemSorter(
            config.getPriorityItems(),
            config.isPreventOverfill(),
            config.isStackItems()
        );
    }

    private void update() {
        ConfigManager config = plugin.getConfigManager();
        int maxActiveGolems = config.getMaxActiveGolems();

        if (copperGolemType == null) {
            // Log once then skip processing until the type is available
            if (config.isDebug()) {
                plugin.getLogger().warning("Copper golem entity type not found (copper_golem). Skipping processing.");
            }
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            List<LivingEntity> golems = world.getEntities().stream()
                .filter(e -> e.getType() == copperGolemType)
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(golem -> !golem.isDead() && golem.isValid())
                .toList();

            for (LivingEntity golem : golems) {
                if (maxActiveGolems > 0 && activeTasks.size() >= maxActiveGolems) {
                    break;
                }

                UUID golemId = golem.getUniqueId();
                GolemTask task = activeTasks.computeIfAbsent(golemId, id -> new GolemTask(golem));

                processGolemTask(task);
            }
        }

        activeTasks.entrySet().removeIf(entry ->
            entry.getValue().getGolem().isDead() || !entry.getValue().getGolem().isValid()
        );
    }

    private void processGolemTask(GolemTask task) {
        ConfigManager config = plugin.getConfigManager();
        LivingEntity golem = task.getGolem();
        long currentTick = golem.getWorld().getFullTime();

        long ticksSinceLastTransfer = currentTick - task.getLastTransferTick();
        if (ticksSinceLastTransfer < config.getTicksBetweenTransfers() && task.getState() != GolemTask.TaskState.IDLE) {
            return;
        }

        switch (task.getState()) {
            case IDLE:
                searchForCopperChest(task);
                break;

            case SEARCHING_SOURCE:
                searchForCopperChest(task);
                break;

            case MOVING_TO_SOURCE:
                if (isNearLocation(golem.getLocation(), task.getSourceChest().getLocation(), 3.0)) {
                    task.setState(GolemTask.TaskState.COLLECTING_ITEMS);
                }
                break;

            case COLLECTING_ITEMS:
                collectItemsFromChest(task);
                break;

            case SEARCHING_TARGET:
                searchForNormalChest(task);
                break;

            case MOVING_TO_TARGET:
                if (isNearLocation(golem.getLocation(), task.getTargetChest().getLocation(), 3.0)) {
                    task.setState(GolemTask.TaskState.DEPOSITING_ITEMS);
                }
                break;

            case DEPOSITING_ITEMS:
                depositItemsToChest(task);
                break;
        }
    }

    private void searchForCopperChest(GolemTask task) {
        ConfigManager config = plugin.getConfigManager();
        LivingEntity golem = task.getGolem();

        List<Chest> copperChests = chestManager.findCopperChestsNearby(
            golem.getLocation(),
            config.getCopperChestDetection()
        );

        Chest targetChest = null;
        for (Chest chest : copperChests) {
            if (chestManager.hasItems(chest)) {
                targetChest = chest;
                break;
            }
        }

        if (targetChest != null) {
            task.setSourceChest(targetChest);
            task.setState(GolemTask.TaskState.MOVING_TO_SOURCE);
            moveGolemToLocation(golem, targetChest.getLocation());

            if (config.isDebug()) {
                plugin.getLogger().info("Golem " + golem.getUniqueId() + " found copper chest at " + targetChest.getLocation());
            }
        } else {
            task.setState(GolemTask.TaskState.IDLE);
        }
    }

    private void collectItemsFromChest(GolemTask task) {
        ConfigManager config = plugin.getConfigManager();
        LivingEntity golem = task.getGolem();
        Chest chest = task.getSourceChest();

        if (chest == null || !chestManager.hasItems(chest)) {
            task.reset();
            return;
        }

        List<ItemStack> items = chestManager.extractItemsFromChest(
            chest,
            config.getMaxStacks(),
            config.getMaxTotalItems()
        );

        if (items.isEmpty()) {
            task.reset();
            return;
        }

        if (config.isSortingEnabled()) {
            items = itemSorter.sortItems(items);
        }

        task.setCarriedItems(items);
        task.setState(GolemTask.TaskState.SEARCHING_TARGET);
        task.setLastTransferTick(golem.getWorld().getFullTime());

        if (config.isDebug()) {
            plugin.getLogger().info("Golem " + golem.getUniqueId() + " collected " + items.size() + " item stacks");
        }
    }

    private void searchForNormalChest(GolemTask task) {
        ConfigManager config = plugin.getConfigManager();
        LivingEntity golem = task.getGolem();

        List<Chest> normalChests = chestManager.findNormalChestsNearby(
            golem.getLocation(),
            config.getNormalChestDetection()
        );

        Chest bestChest = null;
        for (Chest chest : normalChests) {
            if (chestManager.getEmptySlots(chest) > 0) {
                bestChest = chest;
                break;
            }
        }

        if (bestChest != null) {
            task.setTargetChest(bestChest);
            task.setState(GolemTask.TaskState.MOVING_TO_TARGET);
            moveGolemToLocation(golem, bestChest.getLocation());

            if (config.isDebug()) {
                plugin.getLogger().info("Golem " + golem.getUniqueId() + " found normal chest at " + bestChest.getLocation());
            }
        } else {
            if (config.isDebug()) {
                plugin.getLogger().warning("Golem " + golem.getUniqueId() + " couldn't find suitable chest, returning items");
            }
            returnItemsToSource(task);
        }
    }

    private void depositItemsToChest(GolemTask task) {
        ConfigManager config = plugin.getConfigManager();
        LivingEntity golem = task.getGolem();
        Chest chest = task.getTargetChest();

        if (chest == null) {
            returnItemsToSource(task);
            return;
        }

        List<ItemStack> items = new ArrayList<>(task.getCarriedItems());
        int itemsDeposited = 0;

        for (ItemStack item : items) {
            if (itemSorter.canFitInInventory(chest.getInventory(), item)) {
                int deposited = itemSorter.addItemToInventory(chest.getInventory(), item);
                itemsDeposited += deposited;
            }
        }

        task.clearCarriedItems();
        task.reset();

        if (config.isDebug()) {
            plugin.getLogger().info("Golem " + golem.getUniqueId() + " deposited " + itemsDeposited + " items");
        }
    }

    private void returnItemsToSource(GolemTask task) {
        Chest sourceChest = task.getSourceChest();
        if (sourceChest != null) {
            for (ItemStack item : task.getCarriedItems()) {
                sourceChest.getInventory().addItem(item);
            }
        }
        task.reset();
    }

    private void moveGolemToLocation(LivingEntity golem, Location target) {
        ConfigManager config = plugin.getConfigManager();

        double speed = config.getMovementSpeed();
        if (golem.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            golem.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25 * speed);
        }
    }

    private boolean isNearLocation(Location loc1, Location loc2, double distance) {
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            return false;
        }
        return loc1.distance(loc2) <= distance;
    }

    private EntityType resolveCopperGolemType() {
        // Try enum constant first (if present in current API)
        try {
            return EntityType.valueOf("COPPER_GOLEM");
        } catch (IllegalArgumentException ignored) {}

        // Try registry key lookup by identifier "copper_golem"
        try {
            EntityType byKey = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("copper_golem"));
            if (byKey != null) return byKey;
        } catch (Throwable ignored) {}

        // Not available on this server/api
        return null;
    }
}
