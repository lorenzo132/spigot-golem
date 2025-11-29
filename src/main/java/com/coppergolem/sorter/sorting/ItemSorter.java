package com.coppergolem.sorter.sorting;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemSorter {

    private final List<Material> priorityItems;
    private final boolean preventOverfill;
    private final boolean stackItems;

    public ItemSorter(List<Material> priorityItems, boolean preventOverfill, boolean stackItems) {
        this.priorityItems = new ArrayList<>(priorityItems);
        this.preventOverfill = preventOverfill;
        this.stackItems = stackItems;
    }

    public List<ItemStack> sortItems(List<ItemStack> items) {
        if (!stackItems) {
            return sortByPriority(items);
        }

        Map<Material, List<ItemStack>> groupedItems = new HashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;

            groupedItems.computeIfAbsent(item.getType(), k -> new ArrayList<>()).add(item);
        }

        List<ItemStack> stackedItems = new ArrayList<>();
        for (Map.Entry<Material, List<ItemStack>> entry : groupedItems.entrySet()) {
            stackedItems.addAll(stackItemList(entry.getValue()));
        }

        return sortByPriority(stackedItems);
    }

    private List<ItemStack> stackItemList(List<ItemStack> items) {
        if (items.isEmpty()) return items;

        List<ItemStack> result = new ArrayList<>();
        Material type = items.get(0).getType();
        int maxStackSize = type.getMaxStackSize();
        int currentAmount = 0;

        for (ItemStack item : items) {
            currentAmount += item.getAmount();
        }

        while (currentAmount > 0) {
            int stackSize = Math.min(currentAmount, maxStackSize);
            ItemStack stack = new ItemStack(type, stackSize);
            result.add(stack);
            currentAmount -= stackSize;
        }

        return result;
    }

    private List<ItemStack> sortByPriority(List<ItemStack> items) {
        items.sort((item1, item2) -> {
            int priority1 = getPriority(item1.getType());
            int priority2 = getPriority(item2.getType());

            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }

            return item1.getType().name().compareTo(item2.getType().name());
        });

        return items;
    }

    private int getPriority(Material material) {
        int index = priorityItems.indexOf(material);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    public boolean canFitInInventory(Inventory inventory, ItemStack item) {
        if (!preventOverfill) {
            return true;
        }

        int availableSpace = 0;
        for (ItemStack slot : inventory.getContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                availableSpace += item.getType().getMaxStackSize();
            } else if (slot.getType() == item.getType() && slot.getAmount() < slot.getMaxStackSize()) {
                availableSpace += (slot.getMaxStackSize() - slot.getAmount());
            }
        }

        return availableSpace >= item.getAmount();
    }

    public ItemStack findBestSlotForItem(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (slot == null || slot.getType() == Material.AIR) {
                continue;
            }

            if (slot.getType() == item.getType() && slot.getAmount() < slot.getMaxStackSize()) {
                return slot;
            }
        }

        return null;
    }

    public int addItemToInventory(Inventory inventory, ItemStack item) {
        ItemStack existingStack = findBestSlotForItem(inventory, item);

        if (existingStack != null) {
            int availableSpace = existingStack.getMaxStackSize() - existingStack.getAmount();
            int amountToAdd = Math.min(availableSpace, item.getAmount());
            existingStack.setAmount(existingStack.getAmount() + amountToAdd);
            return amountToAdd;
        }

        HashMap<Integer, ItemStack> result = inventory.addItem(item);
        if (result.isEmpty()) {
            return item.getAmount();
        } else {
            ItemStack remaining = result.get(0);
            return item.getAmount() - remaining.getAmount();
        }
    }
}
