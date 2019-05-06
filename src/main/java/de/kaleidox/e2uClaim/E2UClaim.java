package de.kaleidox.e2uClaim;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import de.kaleidox.e2uClaim.chat.Chat;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.exception.PluginEnableException;
import de.kaleidox.e2uClaim.lock.LockManager;
import de.kaleidox.e2uClaim.util.BukkitUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class E2UClaim extends JavaPlugin {
    public static final String PATH_BASE = "plugins/e2uClaim/";

    public static E2UClaim INSTANCE;
    public static Logger LOGGER;

    private static Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        Player player = BukkitUtil.getPlayer(sender);

        if (player.getWorld().getName().equals("configVersion")) return true;

        switch (label.toLowerCase()) {
            case "lock":
                LockManager.INSTANCE.requestLock(player);
                break;
            case "unlock":
                LockManager.INSTANCE.requestUnlock(player);
                break;
        }

        return super.onCommand(sender, command, label, args);
    }

    @Nullable
    @Override
    public PluginCommand getCommand(@NotNull String name) {
        return super.getCommand(name);
    }

    @Override
    public void onLoad() {
        INSTANCE = this;
        LOGGER = getLogger();

        super.onLoad();
        saveConfig();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        LockManager.INSTANCE.terminate();

        configs.forEach((name, config) -> {
            try {
                File file = new File(PATH_BASE + name + ".yml");
                file.createNewFile();

                config.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void onEnable() {
        try {
            Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
            Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (worldEdit != null && worldGuard != null) {
                LOGGER.info("Detected WorldEdit and WorldGuard! Forcing WorldEdit and WorldGuard enabling...");
                worldEdit.getPluginLoader().enablePlugin(worldEdit);
                worldGuard.getPluginLoader().enablePlugin(worldGuard);
                LOGGER.info("Finished loading WorldEdit and WorldGuard. Removing WorldGuard SignChangeEvent listener...");
                SignChangeEvent.getHandlerList().unregister(worldGuard);
                LOGGER.info("Disabled WorldGuard SignChangeEvent listener! Continuing enabling...");
            }

            super.onEnable();

            saveDefaultConfig();
            configs.put("config", getConfig());

            World configVersion = Bukkit.getWorld("configVersion");
            if (configVersion != null)
                E2UClaim.LOGGER.warning("World with name \"configVersion\" detected. This world will be ignored by e2uClaim.");

            Bukkit.getPluginManager().registerEvents(LockManager.INSTANCE, this);
            LockManager.INSTANCE.init();

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(LockManager.INSTANCE::terminate, 5, 5, TimeUnit.MINUTES);
        } catch (PluginEnableException e) {
            LOGGER.severe("Unable to load " + toString() + ": " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public static FileConfiguration getConfig(String name) {
        return configs.compute(name, (k, v) -> {
            if (v != null) return v;

            try {
                File file = new File(PATH_BASE + name + ".yml");
                file.createNewFile();

                YamlConfiguration configuration = new YamlConfiguration();
                configuration.load(file);
                return configuration;
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public enum Permission {
        CREATE_LOCK("e2uclaim.lock", "You are not allowed to create locks!"),
        OVERRIDE_LOCK("e2uclaim.mod", "");

        @NotNull private final String node;
        @Nullable private final String customMissingMessage;

        Permission(@NotNull String node, @Nullable String customMissingMessage) {
            this.node = node;
            this.customMissingMessage = customMissingMessage;
        }

        Permission(@NotNull String node) {
            this(node, null);
        }

        public boolean check(CommandSender user) {
            if (user.hasPermission(node)) return true;
            if (customMissingMessage == null)
                Chat.message(user, MessageType.ERROR, "You are missing the required permission: %s", node);
            else if (customMissingMessage.isEmpty()) return false;
            else Chat.message(user, MessageType.ERROR, customMissingMessage);
            return false;
        }
    }
}
