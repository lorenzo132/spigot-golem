package com.coppergolem.sorter.transfer;

import com.coppergolem.sorter.sorting.ItemCategory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
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

        ItemCategory category = ItemCategory.getCategory(items.get(0));
        
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
        ItemCategory bySign = getSignCategory(chest);
        if (bySign != null) return bySign;

        Map<ItemCategory, Integer> categoryCount = new HashMap<>();
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                ItemCategory cat = ItemCategory.getCategory(item);
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

        return dominant != null ? dominant : ItemCategory.MISC_OVERFLOW;
    }

    private ItemCategory getSignCategory(Chest chest) {
        Block chestBlock = chest.getBlock();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block signBlock = chestBlock.getRelative(face);
            Material type = signBlock.getType();
            if (!type.name().endsWith("_WALL_SIGN") && !type.name().endsWith("_SIGN")) continue;
            if (signBlock.getState() instanceof Sign) {
                Sign sign = (Sign) signBlock.getState();
                String line = readSignLine(sign, 1);
                ItemCategory byName = ItemCategory.fromDisplayName(line);
                if (byName != null) return byName;
            }
        }
        return null;
    }

    public boolean hasMisplacedItems(Chest chest) {
        ItemCategory chestCategory = getChestCategory(chest);
        if (chestCategory == null) return false;
        Inventory inv = chest.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            ItemCategory ic = ItemCategory.getCategory(item);
            if (ic != chestCategory) return true;
        }
        return false;
    }

    public List<ItemStack> extractMisplacedItemsFromChest(Chest chest, int maxStacks, int maxTotalItems) {
        List<ItemStack> extractedItems = new ArrayList<>();
        Inventory inventory = chest.getInventory();
        int stackCount = 0;
        int totalItemCount = 0;

        ItemCategory chestCategory = getChestCategory(chest);
        if (chestCategory == null) return extractedItems;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            if (stackCount >= maxStacks) break;
            if (maxTotalItems > 0 && totalItemCount >= maxTotalItems) break;

            ItemCategory ic = ItemCategory.getCategory(item);
            if (ic == chestCategory) continue; // correct chest, skip

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
                    writeSignLine(sign, 1, category.getDisplayName());
                    sign.update();
                }
                return;
            }
        }
    }

    private String readSignLine(Sign sign, int index) {
        try {
            // Newer API path: sign.getSide(Side.FRONT).line(index) -> Component
            Class<?> sideEnum = Class.forName("org.bukkit.block.sign.Side");
            Object front = sideEnum.getField("FRONT").get(null);
            Object side = sign.getClass().getMethod("getSide", sideEnum).invoke(sign, front);
            Object comp = side.getClass().getMethod("line", int.class).invoke(side, index);
            if (comp != null) {
                // Try Component#toString or plain content via PlainTextComponentSerializer if present
                try {
                    Class<?> serializer = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                    Object inst = serializer.getMethod("plainText").invoke(null);
                    String text = (String) serializer.getMethod("serialize", Class.forName("net.kyori.adventure.text.Component")).invoke(inst, comp);
                    return text;
                } catch (Throwable ignored) {
                    return comp.toString();
                }
            }
        } catch (Throwable ignored) {}
        try {
            // Legacy fallback via reflection: Sign#getLine(int)
            return (String) sign.getClass().getMethod("getLine", int.class).invoke(sign, index);
        } catch (Throwable ignored) {}
        return "";
    }

    private void writeSignLine(Sign sign, int index, String text) {
        try {
            // Newer API path: sign.getSide(Side.FRONT).line(index, Component.text(text))
            Class<?> sideEnum = Class.forName("org.bukkit.block.sign.Side");
            Object front = sideEnum.getField("FRONT").get(null);
            Object side = sign.getClass().getMethod("getSide", sideEnum).invoke(sign, front);
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Object component = Class.forName("net.kyori.adventure.text.Component")
                .getMethod("text", String.class).invoke(null, text);
            side.getClass().getMethod("line", int.class, componentClass).invoke(side, index, component);
            return;
        } catch (Throwable ignored) {}
        try {
            // Legacy fallback: Sign#setLine(int, String)
            sign.getClass().getMethod("setLine", int.class, String.class).invoke(sign, index, text);
        } catch (Throwable ignored) {}
    }
}
