package com.coppergolem.sorter.transfer;

import com.coppergolem.sorter.sorting.ItemCategory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestManager {

    public List<Chest> findCopperChestsNearby(Location location, double range) {
        List<Chest> copperChests = new ArrayList<>();
        int rangeInt = (int) Math.ceil(range);

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    Location checkLoc = location.clone().add(x, y, z);

                    if (location.distance(checkLoc) > range) continue;

                    Block block = checkLoc.getBlock();
                    if (isCopperChest(block)) {
                        if (block.getState() instanceof Chest) {
                            copperChests.add((Chest) block.getState());
                        }
                    }
                }
            }
        }

        return copperChests;
    }

    public List<Chest> findNormalChestsNearby(Location location, double range) {
        List<Chest> normalChests = new ArrayList<>();
        int rangeInt = (int) Math.ceil(range);

        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    Location checkLoc = location.clone().add(x, y, z);

                    if (location.distance(checkLoc) > range) continue;

                    Block block = checkLoc.getBlock();
                    if (isNormalChest(block)) {
                        if (block.getState() instanceof Chest) {
                            normalChests.add((Chest) block.getState());
                        }
                    }
                }
            }
        }

        return normalChests;
    }

    private boolean isCopperChest(Block block) {
        Material type = block.getType();
        return type == Material.EXPOSED_COPPER ||
               type == Material.WEATHERED_COPPER ||
               type == Material.OXIDIZED_COPPER ||
               type == Material.COPPER_BLOCK ||
               type.name().contains("COPPER");
    }

    private boolean isNormalChest(Block block) {
        Material type = block.getType();
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }

    public List<ItemStack> extractItemsFromChest(Chest chest, int maxStacks, int maxTotalItems) {
        List<ItemStack> extractedItems = new ArrayList<>();
        Inventory inventory = chest.getInventory();
        int stackCount = 0;
        int totalItemCount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);

            if (item == null || item.getType() == Material.AIR) continue;

            if (stackCount >= maxStacks) break;
            if (maxTotalItems > 0 && totalItemCount >= maxTotalItems) break;

            int amountToTake = item.getAmount();
            if (maxTotalItems > 0 && (totalItemCount + amountToTake) > maxTotalItems) {
                amountToTake = maxTotalItems - totalItemCount;
            }

            ItemStack extractedItem = item.clone();
            extractedItem.setAmount(amountToTake);
            extractedItems.add(extractedItem);

            if (amountToTake >= item.getAmount()) {
                inventory.setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - amountToTake);
            }

            stackCount++;
            totalItemCount += amountToTake;
        }

        return extractedItems;
    }

    public boolean hasItems(Chest chest) {
        Inventory inventory = chest.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    public int getEmptySlots(Chest chest) {
        Inventory inventory = chest.getInventory();
        int emptySlots = 0;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }

        return emptySlots;
    }

    public Chest findBestChestForItems(List<Chest> chests, List<ItemStack> items) {
        if (items.isEmpty()) return null;

        ItemCategory category = ItemCategory.getCategory(items.get(0).getType());
        
        Chest sameCategory = null;
        Chest emptyChest = null;
        Chest anyChest = null;

        for (Chest chest : chests) {
            int emptySlots = getEmptySlots(chest);
            if (emptySlots == 0) continue;

            if (emptySlots == chest.getInventory().getSize()) {
                if (emptyChest == null) emptyChest = chest;
                continue;
            }

            ItemCategory chestCategory = getChestCategory(chest);
            if (chestCategory == category) {
                return chest;
            }

            if (anyChest == null) anyChest = chest;
        }

        return sameCategory != null ? sameCategory : (emptyChest != null ? emptyChest : anyChest);
    }

    private ItemCategory getChestCategory(Chest chest) {
        Map<ItemCategory, Integer> categoryCount = new HashMap<>();
        
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                ItemCategory cat = ItemCategory.getCategory(item.getType());
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + item.getAmount());
            }
        }

        ItemCategory dominant = null;
        int maxCount = 0;
        for (Map.Entry<ItemCategory, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominant = entry.getKey();
            }
        }

        return dominant != null ? dominant : ItemCategory.MISC;
    }

    public void placeSignOnChest(Chest chest, ItemCategory category) {
        Block chestBlock = chest.getBlock();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

        for (BlockFace face : faces) {
            Block signBlock = chestBlock.getRelative(face);
            if (signBlock.getType() == Material.AIR) {
                signBlock.setType(Material.OAK_WALL_SIGN);
                if (signBlock.getBlockData() instanceof WallSign) {
                    WallSign wallSign = (WallSign) signBlock.getBlockData();
                    wallSign.setFacing(face);
                    signBlock.setBlockData(wallSign);
                }
                if (signBlock.getState() instanceof Sign) {
                    Sign sign = (Sign) signBlock.getState();
                    sign.setLine(1, category.getDisplayName());
                    sign.update();
                }
                return;
            }
        }
    }
}
