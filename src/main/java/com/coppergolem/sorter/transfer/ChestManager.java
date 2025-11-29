package com.coppergolem.sorter.transfer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
}
