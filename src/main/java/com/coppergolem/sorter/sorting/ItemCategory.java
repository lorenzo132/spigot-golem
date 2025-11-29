package com.coppergolem.sorter.sorting;

import org.bukkit.Material;

public enum ItemCategory {
    BUILDING_BLOCKS("Building Blocks"),
    TOOLS("Tools"),
    FOOD("Food"),
    COMBAT("Combat"),
    REDSTONE("Redstone"),
    DECORATIONS("Decorations"),
    ORES("Ores"),
    INGOTS("Ingots"),
    ARMOR("Armor"),
    TRANSPORT("Transport"),
    POTIONS("Potions"),
    FARMING("Farming"),
    FISHING("Fishing"),
    MISC("Misc");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemCategory getCategory(Material material) {
        String name = material.name();

        // Food detection
        if (material.isEdible()) return FOOD;

        // Ores
        if (name.endsWith("_ORE") || name.equals("NETHER_GOLD_ORE") || name.equals("ANCIENT_DEBRIS")) {
            return ORES;
        }

        // Ingots / nuggets / raw materials
        if (name.endsWith("_INGOT") || name.endsWith("_NUGGET") || name.startsWith("RAW_")) {
            return INGOTS;
        }

        // Armor
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
            name.endsWith("_HORSE_ARMOR")) {
            return ARMOR;
        }

        // Combat (weapons, projectiles, shields)
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("BOW") || name.equals("CROSSBOW") ||
            name.equals("ARROW") || name.equals("SPECTRAL_ARROW") || name.equals("TIPPED_ARROW") || name.equals("SHIELD")) {
            return COMBAT;
        }

        // Tools & utilities
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("SHEARS") ||
            name.equals("FISHING_ROD") || name.equals("FLINT_AND_STEEL") || name.equals("ELYTRA")) {
            return TOOLS;
        }

        // Potions and related
        if (name.contains("POTION") || name.equals("SPLASH_POTION") || name.equals("LINGERING_POTION") || name.equals("TIPPED_ARROW")) {
            return POTIONS;
        }

        // Farming / crops
        if (name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT") ||
            name.contains("SEED") || name.equals("BONE_MEAL") || name.contains("MELON") || name.contains("PUMPKIN")) {
            return FARMING;
        }

        // Fishing
        if (name.contains("FISH") || name.equals("FISHING_ROD") || name.equals("BOWL") && material == Material.MUSHROOM_STEW) {
            return FISHING;
        }

        // Transport / vehicles
        if (name.contains("MINECART") || name.contains("BOAT") || name.equals("SADDLE") || name.equals("LEAD") || name.equals("HORSE_ARMOR")) {
            return TRANSPORT;
        }

        // Redstone / machinery
        if (name.contains("REDSTONE") || name.contains("PISTON") || name.contains("REPEATER") || name.contains("COMPARATOR") ||
            name.contains("OBSERVER") || name.equals("LEVER") || name.equals("BUTTON") || name.contains("PRESSURE_PLATE") || name.contains("RAIL")) {
            return REDSTONE;
        }

        // Decorations (banners, carpets, flowers, frames)
        if (name.contains("BANNER") || name.contains("CARPET") || name.contains("PAINTING") || name.contains("FLOWER") ||
            name.contains("PLANT") || name.equals("ITEM_FRAME") || name.contains("POTTERY") || name.contains("POTTED")) {
            return DECORATIONS;
        }

        // Building blocks fallback: prefer block types
        try {
            if (material.isBlock()) {
                return BUILDING_BLOCKS;
            }
        } catch (NoSuchMethodError ignored) {
            // Older APIs may not have isBlock(); continue
        }

        return MISC;
    }
}
