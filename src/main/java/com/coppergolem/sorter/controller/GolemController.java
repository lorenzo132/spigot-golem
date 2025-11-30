package com.coppergolem.sorter.controller;

import com.coppergolem.sorter.CopperGolemSorterPlugin;
import com.coppergolem.sorter.config.ConfigManager;
import com.coppergolem.sorter.model.GolemTask;
import com.coppergolem.sorter.sorting.ItemCategory;
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
    // Remember preferred destination chests per item category (FIFO capped)
    private final Map<ItemCategory, LinkedList<String>> categoryChestMemory = new HashMap<>();

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
            task.setSourceType(GolemTask.SourceType.COPPER);
            task.setState(GolemTask.TaskState.MOVING_TO_SOURCE);
            moveGolemToLocation(golem, targetChest.getLocation());

            if (config.isDebug()) {
                plugin.getLogger().info("Golem " + golem.getUniqueId() + " found copper chest at " + targetChest.getLocation());
            }
        } else {
            // If no copper chest, try reorganizing nearby normal chests for misplaced items
            if (config.isReorganizeChests()) {
                List<Chest> normalChests = chestManager.findNormalChestsNearby(
                    golem.getLocation(),
                    config.getNormalChestDetection()
                );
                for (Chest chest : normalChests) {
                    if (chestManager.hasItems(chest) && chestManager.hasMisplacedItems(chest)) {
                        task.setSourceChest(chest);
                        task.setSourceType(GolemTask.SourceType.NORMAL_REORG);
                        task.setState(GolemTask.TaskState.MOVING_TO_SOURCE);
                        moveGolemToLocation(golem, chest.getLocation());
                        if (config.isDebug()) {
                            plugin.getLogger().info("Golem " + golem.getUniqueId() + " found misplaced items in chest at " + chest.getLocation());
                        }
                        return;
                    }
                }
            }
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

        List<ItemStack> items;
        if (task.getSourceType() == GolemTask.SourceType.NORMAL_REORG) {
            items = chestManager.extractMisplacedItemsFromChest(
                chest,
                config.getMaxStacks(),
                config.getMaxTotalItems()
            );
        } else {
            items = chestManager.extractItemsFromChest(
                chest,
                config.getMaxStacks(),
                config.getMaxTotalItems()
            );
        }

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

        // We will distribute items per-category during deposit, ignoring a single target chest
        if (!normalChests.isEmpty()) {
            task.setTargetChest(null);
            task.setState(GolemTask.TaskState.DEPOSITING_ITEMS);
        } else {
            if (config.isDebug()) {
                plugin.getLogger().warning("Golem " + golem.getUniqueId() + " couldn't find any chests, returning items");
            }
            returnItemsToSource(task);
        }
    }

    private void depositItemsToChest(GolemTask task) {
        ConfigManager config = plugin.getConfigManager();
        LivingEntity golem = task.getGolem();
        List<ItemStack> items = new ArrayList<>(task.getCarriedItems());

        List<Chest> normalChests = chestManager.findNormalChestsNearby(
            golem.getLocation(),
            config.getNormalChestDetection()
        );

        List<Chest> copperChests = chestManager.findCopperChestsNearby(
            golem.getLocation(),
            config.getCopperChestDetection()
        );

        List<ItemStack> remaining = new ArrayList<>();

        for (ItemStack item : items) {
            ItemCategory cat = ItemCategory.getCategory(item);

            // 1) Try remembered chests first (normal then copper)
            Chest remembered = getRememberedChestWithSpace(cat, normalChests, item);
            if (remembered == null) {
                remembered = getRememberedChestWithSpace(cat, copperChests, item);
            }
            if (remembered != null) {
                itemSorter.addItemToInventory(remembered.getInventory(), item);
                chestManager.placeSignOnChest(remembered, cat);
                rememberChest(cat, remembered);
                continue;
            }

            // 2) Pick best category chest among normals
            Chest categoryChest = chestManager.findBestChestForItems(normalChests, Collections.singletonList(item));
            if (categoryChest != null && itemSorter.canFitInInventory(categoryChest.getInventory(), item)) {
                itemSorter.addItemToInventory(categoryChest.getInventory(), item);
                chestManager.placeSignOnChest(categoryChest, cat);
                rememberChest(cat, categoryChest);
                continue;
            }

            // 3) Fallback to copper chests
            Chest copper = chestManager.findBestChestForItems(copperChests, Collections.singletonList(item));
            if (copper != null && itemSorter.canFitInInventory(copper.getInventory(), item)) {
                itemSorter.addItemToInventory(copper.getInventory(), item);
                chestManager.placeSignOnChest(copper, cat);
                rememberChest(cat, copper);
                continue;
            }

            // 4) No space anywhere, keep item
            remaining.add(item);
        }

        if (remaining.isEmpty()) {
            task.clearCarriedItems();
            task.reset();
        } else {
            // Keep only unplaced items; retry later automatically
            task.setCarriedItems(remaining);

            if (config.isDebug()) {
                plugin.getLogger().warning("Golem is holding " + remaining.size() + " unplaced items (waiting for space)");
            }
        }
    }

    private Chest getRememberedChestWithSpace(ItemCategory category, List<Chest> candidates, ItemStack item) {
        LinkedList<String> keys = categoryChestMemory.get(category);
        if (keys == null || keys.isEmpty()) return null;

        for (String key : keys) {
            for (Chest chest : candidates) {
                if (key.equals(chestKey(chest)) && itemSorter.canFitInInventory(chest.getInventory(), item)) {
                    return chest;
                }
            }
        }
        return null;
    }

    private void rememberChest(ItemCategory category, Chest chest) {
        int limit = plugin.getConfigManager().getMaxCategoryChestsMemory();
        LinkedList<String> keys = categoryChestMemory.computeIfAbsent(category, c -> new LinkedList<>());
        String key = chestKey(chest);
        // Move to most-recent position
        keys.remove(key);
        keys.addLast(key);
        while (keys.size() > Math.max(1, limit)) {
            keys.removeFirst();
        }
    }

    private String chestKey(Chest chest) {
        Location l = chest.getLocation();
        return l.getWorld().getUID() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
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
