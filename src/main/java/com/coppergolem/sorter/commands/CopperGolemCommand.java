package com.coppergolem.sorter.commands;

import com.coppergolem.sorter.CopperGolemSorterPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CopperGolemCommand implements CommandExecutor {

    private final CopperGolemSorterPlugin plugin;

    public CopperGolemCommand(CopperGolemSorterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Copper Golem Sorter v" + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Usage: /coppergolem reload");
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

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /coppergolem reload");
        return true;
    }
}
