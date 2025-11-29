package com.coppergolem.sorter;

import com.coppergolem.sorter.commands.CopperGolemCommand;
import com.coppergolem.sorter.config.ConfigManager;
import com.coppergolem.sorter.controller.GolemController;
import org.bukkit.plugin.java.JavaPlugin;

public class CopperGolemSorterPlugin extends JavaPlugin {

    private static CopperGolemSorterPlugin instance;
    private ConfigManager configManager;
    private GolemController golemController;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        golemController = new GolemController(this);
        golemController.start();

        getCommand("coppergolem").setExecutor(new CopperGolemCommand(this));

        getLogger().info("Copper Golem Sorter has been enabled!");
        getLogger().info("Copper golems will now sort items from copper chests to normal chests.");
    }

    @Override
    public void onDisable() {
        if (golemController != null) {
            golemController.stop();
        }

        getLogger().info("Copper Golem Sorter has been disabled!");
    }

    public static CopperGolemSorterPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GolemController getGolemController() {
        return golemController;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        configManager.loadConfig();
        golemController.reload();
        getLogger().info("Configuration reloaded!");
    }
}
