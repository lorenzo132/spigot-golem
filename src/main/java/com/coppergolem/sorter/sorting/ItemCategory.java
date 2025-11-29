package com.coppergolem.sorter.sorting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public enum ItemCategory {
    RARE_METALS("01 Rare Metals"),
    ORES_RAW("02 Ores (Raw)"),
    PROCESSED_METALS("03 Processed Metals"),

    TOOLS_UNENCHANTED("04 Tools - Unench."),
    TOOLS_ENCHANTED("04 Tools - Ench."),
    ANVILS_GRINDSTONES("04 Anvils & Grind."),

    WEAPONS_ARMOR_REGULAR("05 Weapons & Armor"),
    WEAPONS_ARMOR_ENCHANTED("05 Armor - Ench."),

    FARMING_ITEMS("06 Farming Items"),

    FOOD_RAW("07 Food - Raw"),
    FOOD_COOKED("07 Food - Cooked"),
    FOOD_PREPARED("07 Food - Prepared"),

    POISONS_HAZARDOUS("08 Poisons & Hazards"),

    MOBS_COMBAT_DROPS("09 Mob & Boss Drops"),

    REDSTONE_SMALL("10 Redstone - Small"),
    REDSTONE_MACHINES("10 Redstone - Machines"),

    ENCHANTING_MAGIC("11 Enchanting & Magic"),
    BREWING_INGREDIENTS("12 Brewing Ingredients"),

    NETHER_BLOCKS("13 Nether - Blocks"),
    NETHER_RESOURCES("13 Nether - Resources"),

    END_MATERIALS("14 End Materials"),
    CRAFTING_MATERIALS("15 Crafting Materials"),
    BLOCKS_BUILDING("16 Blocks - Building"),
    BLOCKS_DECORATIVE("17 Decorative & Furniture"),
    TRANSPORTATION_UTILITY("18 Transport & Utility"),
    POTIONS_FINISHED("19 Potions & Bottles"),
    BUILDING_EXTRAS_MISC("20 Building Extras"),
    MISC_OVERFLOW("21 Misc & Overflow"),
    TRASH_RECYCLE("22 Trash / Recycle"),
    SHULKER_BULK("23 Shulker Bulk");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemCategory fromDisplayName(String name) {
        if (name == null) return null;
        for (ItemCategory c : values()) {
            if (c.displayName.equalsIgnoreCase(name.trim())) {
                return c;
            }
        }
        return null;
    }

    public static ItemCategory getCategory(ItemStack stack) {
        if (stack == null) return MISC_OVERFLOW;

        Material material = stack.getType();
        String name = material.name();
        ItemMeta meta = stack.getItemMeta();

        // 23 - Shulker bulk
        if (name.endsWith("SHULKER_BOX")) return SHULKER_BULK;

        // 22 - Trash: not auto-assigned (requires config/pipeline). Fallback later.

        // 19 - Finished potions & bottles
        if (name.equals("GLASS_BOTTLE")) return POTIONS_FINISHED;
        if (name.contains("POTION")) {
            PotionType type = getPotionType(meta);
            if (type == PotionType.POISON || type == PotionType.HARMING) {
                return POISONS_HAZARDOUS;
            }
            return POTIONS_FINISHED;
        }

        // Tipped arrows
        if (name.equals("TIPPED_ARROW")) {
            PotionType type = getPotionType(meta);
            if (type == PotionType.POISON || type == PotionType.HARMING) {
                return POISONS_HAZARDOUS;
            }
            return WEAPONS_ARMOR_REGULAR; // neutral tips considered combat
        }

        // 08 - Poisons & Hazardous
        if (name.equals("POISONOUS_POTATO") || name.equals("SPIDER_EYE") || name.equals("FERMENTED_SPIDER_EYE") ||
            name.equals("DRAGON_BREATH") || name.equals("TNT")) {
            return POISONS_HAZARDOUS;
        }

        // 04 - Anvils & Grindstones
        if (name.endsWith("ANVIL") || name.equals("GRINDSTONE")) {
            return ANVILS_GRINDSTONES;
        }

        // 04 - Tools (enchanted/unenchanted)
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") || name.endsWith("_HOE") ||
            name.equals("SHEARS") || name.equals("FISHING_ROD") || name.equals("FLINT_AND_STEEL") || name.equals("ELYTRA")) {
            boolean enchanted = meta != null && meta.hasEnchants();
            return enchanted ? TOOLS_ENCHANTED : TOOLS_UNENCHANTED;
        }

        // 05 - Weapons & Armor (enchanted vs regular)
        if (name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("SHIELD") ||
            name.equals("ARROW") || name.equals("SPECTRAL_ARROW") ||
            name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
            name.endsWith("_HORSE_ARMOR")) {
            boolean enchanted = meta != null && meta.hasEnchants();
            return enchanted ? WEAPONS_ARMOR_ENCHANTED : WEAPONS_ARMOR_REGULAR;
        }

        // 02 - Ores (Raw)
        if (name.endsWith("_ORE") || name.startsWith("DEEPSLATE_") && name.endsWith("_ORE") || name.startsWith("RAW_")) {
            return ORES_RAW;
        }

        // 03 - Processed metals & storage
        if (name.endsWith("_INGOT") || name.endsWith("_NUGGET") || name.endsWith("_BLOCK") && (
            name.contains("COPPER") || name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("NETHERITE") || name.contains("GOLD") || name.contains("IRON"))) {
            return PROCESSED_METALS;
        }

        // 01 - Rare metals & valuables
        if (name.equals("DIAMOND") || name.equals("EMERALD") || name.equals("NETHERITE_SCRAP") || name.equals("NETHERITE_INGOT")) {
            return RARE_METALS;
        }

        // 10 - Redstone
        if (name.equals("REDSTONE") || name.equals("REDSTONE_TORCH") || name.equals("REDSTONE_BLOCK") ||
            name.equals("REPEATER") || name.equals("COMPARATOR") || name.equals("LEVER") || name.contains("BUTTON") ||
            name.contains("PRESSURE_PLATE") || name.contains("_RAIL")) {
            return REDSTONE_SMALL;
        }
        if (name.contains("PISTON") || name.equals("OBSERVER") || name.equals("DROPPER") || name.equals("DISPENSER") || name.equals("HOPPER")) {
            return REDSTONE_MACHINES;
        }

        // 11 - Enchanting & Magic
        if (name.equals("ENCHANTED_BOOK") || name.equals("BOOK") || name.equals("EXPERIENCE_BOTTLE") || name.equals("ENCHANTING_TABLE") ||
            name.equals("BOOKSHELF") || name.equals("LAPIS_LAZULI")) {
            return ENCHANTING_MAGIC;
        }

        // 12 - Brewing ingredients
        if (name.equals("NETHER_WART") || name.equals("BLAZE_POWDER") || name.equals("GHAST_TEAR") || name.equals("SUGAR") ||
            name.equals("SPIDER_EYE") || name.equals("FERMENTED_SPIDER_EYE") || name.equals("MAGMA_CREAM") || name.equals("GLISTERING_MELON_SLICE") ||
            name.equals("GUNPOWDER") || name.equals("GLASS_BOTTLE") || name.equals("BREWING_STAND")) {
            return BREWING_INGREDIENTS;
        }

        // 13 - Nether materials
        if (name.contains("NETHERRACK") || name.equals("SOUL_SAND") || name.equals("SOUL_SOIL") || name.equals("BASALT") || name.equals("BLACKSTONE") ||
            name.contains("NETHER_BRICK") || name.equals("ANCIENT_DEBRIS") || name.contains("CRIMSON_") || name.contains("WARPED_")) {
            return NETHER_BLOCKS;
        }
        if (name.contains("QUARTZ") || name.equals("GLOWSTONE_DUST") || name.equals("BLAZE_ROD") || name.equals("NETHER_STAR")) {
            return NETHER_RESOURCES;
        }

        // 14 - End materials
        if (name.equals("END_STONE") || name.contains("PURPUR") || name.equals("CHORUS_FRUIT") || name.equals("CHORUS_FLOWER") || name.equals("DRAGON_EGG")) {
            return END_MATERIALS;
        }

        // 15 - Crafting materials
        if (name.equals("STICK") || name.equals("FEATHER") || name.equals("LEATHER") || name.endsWith("_WOOL") || name.endsWith("_DYE") ||
            name.equals("SLIME_BALL") || name.equals("PAPER") || name.equals("SUGAR") || name.equals("STRING")) {
            return CRAFTING_MATERIALS;
        }

        // 06 - Farming items
        if (name.endsWith("_SEEDS") || name.endsWith("_SAPLING") || name.equals("BONE_MEAL") ||
            name.equals("WHEAT") || name.equals("CARROT") || name.equals("POTATO") || name.equals("BEETROOT") ||
            name.equals("MELON_SLICE") || name.equals("PUMPKIN") || name.equals("BAMBOO") || name.equals("SUGAR_CANE") || name.equals("COCOA_BEANS")) {
            return FARMING_ITEMS;
        }

        // 07 - Food split
        if (material.isEdible()) {
            if (name.startsWith("COOKED_") || name.equals("BREAD") || name.equals("PUMPKIN_PIE") || name.contains("STEW") || name.contains("SOUP") || name.equals("CAKE")) {
                if (name.contains("STEW") || name.contains("SOUP") || name.equals("CAKE") || name.equals("PUMPKIN_PIE")) {
                    return FOOD_PREPARED;
                }
                return FOOD_COOKED;
            }
            return FOOD_RAW;
        }

        // 18 - Transportation & Utility
        if (name.contains("MINECART") || name.contains("BOAT") || name.equals("SADDLE") || name.equals("LEAD") || name.equals("COMPASS") || name.equals("MAP") || name.equals("RECOVERY_COMPASS")) {
            return TRANSPORTATION_UTILITY;
        }

        // 20 - Building extras / misc
        if (name.endsWith("_SIGN") || name.equals("LADDER") || name.equals("SCAFFOLDING") || name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") ||
            name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE")) {
            return BUILDING_EXTRAS_MISC;
        }

        // 17 - Decorative & furniture
        if (name.contains("BANNER") || name.contains("CARPET") || name.equals("PAINTING") || name.equals("ITEM_FRAME") || name.equals("FLOWER_POT") ||
            name.contains("GLOSSY") || name.contains("GILDED") || name.contains("STAIRS") || name.contains("SLAB") || name.contains("GILDED_BLACKSTONE")) {
            return BLOCKS_DECORATIVE;
        }

        // 16 - Building blocks (broad catch for blocks)
        try {
            if (material.isBlock()) {
                return BLOCKS_BUILDING;
            }
        } catch (NoSuchMethodError ignored) {}

        // 09 - Mob drops
        if (name.equals("BONE") || name.equals("STRING") || name.equals("ROTTEN_FLESH") || name.equals("ENDER_PEARL") || name.equals("BLAZE_ROD") ||
            name.equals("GHAST_TEAR") || name.equals("WITHER_SKELETON_SKULL") || name.equals("PHANTOM_MEMBRANE") || name.equals("GUNPOWDER")) {
            return MOBS_COMBAT_DROPS;
        }

        // Fallback
        return MISC_OVERFLOW;
    }

    public static ItemCategory getCategory(Material material) {
        // Fallback for legacy usages where only Material is available
        return getCategory(new ItemStack(material));
    }

    private static PotionType getPotionType(ItemMeta meta) {
        if (!(meta instanceof PotionMeta)) return null;
        PotionMeta pm = (PotionMeta) meta;
        try {
            // Newer APIs via reflection to avoid compile issues across versions
            Object t = pm.getClass().getMethod("getBasePotionType").invoke(pm);
            if (t instanceof PotionType) return (PotionType) t;
        } catch (Throwable ignored) {}
        try {
            // Legacy fallback via reflection: PotionMeta#getBasePotionData().getType()
            Object data = pm.getClass().getMethod("getBasePotionData").invoke(pm);
            if (data != null) {
                Object t = data.getClass().getMethod("getType").invoke(data);
                if (t instanceof PotionType) return (PotionType) t;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
