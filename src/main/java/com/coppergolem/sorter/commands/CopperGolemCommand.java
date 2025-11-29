package com.coppergolem.sorter.commands;

import com.coppergolem.sorter.CopperGolemSorterPlugin;
import com.coppergolem.sorter.sorting.ItemCategory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CopperGolemCommand implements CommandExecutor {

    private final CopperGolemSorterPlugin plugin;

    public CopperGolemCommand(CopperGolemSorterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Copper Golem Sorter v" + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Usage: /coppergolem reload|audit");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("coppergolem.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "Copper Golem Sorter configuration reloaded!");
            return true;
        }

        if (args[0].equalsIgnoreCase("audit")) {
            if (!sender.hasPermission("coppergolem.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            runAudit(sender);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /coppergolem reload|audit");
        return true;
    }

    private void runAudit(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Running category audit... this may take a moment.");

        Map<ItemCategory, List<String>> byCategory = new EnumMap<>(ItemCategory.class);
        List<String> overflow = new ArrayList<>();
        int total = 0;

        for (Material mat : Material.values()) {
            if (mat == Material.AIR) continue;
            if (!isItem(mat)) continue; // skip non-items (pure blocks w/o item form)
            try {
                ItemStack stack = new ItemStack(mat);
                ItemCategory cat = ItemCategory.getCategory(stack);
                byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(mat.name());
                if (cat == ItemCategory.MISC_OVERFLOW) overflow.add(mat.name());
                total++;
            } catch (Throwable ignored) {}
        }

        File dataDir = plugin.getDataFolder();
        if (!dataDir.exists()) dataDir.mkdirs();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = dataDir.toPath().resolve("category-audit-" + ts + ".txt");

        List<String> lines = new ArrayList<>();
        lines.add("CopperGolemSorter Category Audit - " + ts);
        lines.add("Server: " + Bukkit.getVersion());
        lines.add("Total classifiable items: " + total);
        lines.add("");
        for (ItemCategory c : ItemCategory.values()) {
            List<String> items = byCategory.getOrDefault(c, new ArrayList<>());
            lines.add(c.getDisplayName() + " (" + items.size() + ")");
            if (!items.isEmpty()) {
                lines.add(String.join(", ", items));
            }
            lines.add("");
        }
        if (!overflow.isEmpty()) {
            lines.add("Items classified as Misc & Overflow (needs mapping): " + overflow.size());
            lines.add(String.join(", ", overflow));
        }

        try {
            Files.write(out, lines, StandardCharsets.UTF_8);
            sender.sendMessage(ChatColor.GREEN + "Audit complete. Report saved to: " + out.toString());
            sender.sendMessage(ChatColor.YELLOW + "Overflow count: " + overflow.size());
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed to write audit report: " + e.getMessage());
        }
    }

    private boolean isItem(Material mat) {
        // Prefer Material#isItem() if present
        try {
            Object res = Material.class.getMethod("isItem").invoke(mat);
            if (res instanceof Boolean) return (Boolean) res;
        } catch (Throwable ignored) {}
        // Fallback heuristic: not a block or has max stack > 0
        try {
            boolean isBlock = (boolean) Material.class.getMethod("isBlock").invoke(mat);
            if (isBlock) {
                // Many blocks also have item forms; include them by default
                return true;
            }
        } catch (Throwable ignored) {}
        return true;
    }
}
