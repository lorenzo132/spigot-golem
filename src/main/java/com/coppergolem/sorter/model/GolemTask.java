package com.coppergolem.sorter.model;

import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GolemTask {

    private final LivingEntity golem;
    private Chest sourceChest;
    private Chest targetChest;
    private List<ItemStack> carriedItems;
    private TaskState state;
    private long lastTransferTick;

    public enum TaskState {
        IDLE,
        SEARCHING_SOURCE,
        MOVING_TO_SOURCE,
        COLLECTING_ITEMS,
        SEARCHING_TARGET,
        MOVING_TO_TARGET,
        DEPOSITING_ITEMS
    }

    public GolemTask(LivingEntity golem) {
        this.golem = golem;
        this.carriedItems = new ArrayList<>();
        this.state = TaskState.IDLE;
        this.lastTransferTick = 0;
    }

    public LivingEntity getGolem() {
        return golem;
    }

    public Chest getSourceChest() {
        return sourceChest;
    }

    public void setSourceChest(Chest sourceChest) {
        this.sourceChest = sourceChest;
    }

    public Chest getTargetChest() {
        return targetChest;
    }

    public void setTargetChest(Chest targetChest) {
        this.targetChest = targetChest;
    }

    public List<ItemStack> getCarriedItems() {
        return carriedItems;
    }

    public void setCarriedItems(List<ItemStack> carriedItems) {
        this.carriedItems = carriedItems;
    }

    public void addCarriedItem(ItemStack item) {
        this.carriedItems.add(item);
    }

    public void clearCarriedItems() {
        this.carriedItems.clear();
    }

    public boolean hasCarriedItems() {
        return !carriedItems.isEmpty();
    }

    public int getTotalCarriedItemCount() {
        int total = 0;
        for (ItemStack item : carriedItems) {
            total += item.getAmount();
        }
        return total;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public long getLastTransferTick() {
        return lastTransferTick;
    }

    public void setLastTransferTick(long lastTransferTick) {
        this.lastTransferTick = lastTransferTick;
    }

    public void reset() {
        this.sourceChest = null;
        this.targetChest = null;
        this.carriedItems.clear();
        this.state = TaskState.IDLE;
    }
}
