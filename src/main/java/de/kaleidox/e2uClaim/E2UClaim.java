package de.kaleidox.e2uClaim;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import de.kaleidox.e2uClaim.chat.Chat;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.claim.ClaimManager;
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
import org.bukkit.entity.Entity;
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
        Optional<Player> playerOptional = BukkitUtil.getPlayer(sender);
        if (playerOptional.map(Entity::getWorld)
                .map(World::getName)
                .map("configVersion"::equals)
                .orElse(false)) return false;

        switch (label.toLowerCase()) {
            case "lock":
                playerOptional.ifPresent(LockManager.INSTANCE::requestLock);
                break;
            case "unlock":
                playerOptional.ifPresent(LockManager.INSTANCE::requestUnlock);
                break;
            case "claim":
                playerOptional.ifPresent(ClaimManager.INSTANCE::requestClaiming);
                break;
            case "unclaim":
                playerOptional.ifPresent(ClaimManager.INSTANCE::requestUnclaiming);
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

        FileConfiguration config = getConfig();
        if (!config.isSet("defaults.claim-size"))
            config.set("defaults.claim-size", 128);
        saveConfig();

        saveDefaultConfig();
        configs.put("config", getConfig());
    }

    @Override
    public void onDisable() {
        super.onDisable();

        LockManager.INSTANCE.terminate();
        ClaimManager.INSTANCE.terminate();

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

            World configVersion = Bukkit.getWorld("configVersion");
            if (configVersion != null)
                E2UClaim.LOGGER.warning("World with name \"configVersion\" detected. This world will be ignored by e2uClaim.");

            LockManager.INSTANCE.init();
            ClaimManager.INSTANCE.init();
            Bukkit.getPluginManager().registerEvents(LockManager.INSTANCE, this);
            Bukkit.getPluginManager().registerEvents(ClaimManager.INSTANCE, this);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::cycle, 5, 5, TimeUnit.MINUTES);
        } catch (PluginEnableException e) {
            LOGGER.severe("Unable to load " + toString() + ": " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void cycle() {
        configs.forEach((configName, config) -> {
            try {
                File file = new File(PATH_BASE + configName + ".yml");
                file.createNewFile();
                config.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static FileConfiguration getConfig(String name) {
        return configs.compute(name, (k, v) -> { // .compute will place the result of the BiFunction inside the map at the given key.
            if (v != null) return v; // if the value is already set, return it

            try { // if there is no value; create it:
                File file = new File(PATH_BASE + name + ".yml"); // get the file
                file.createNewFile(); // create the file if neccessary

                YamlConfiguration configuration = new YamlConfiguration(); // create the configuration
                configuration.load(file); // load the configuration
                return configuration; // place the configuration inside the map at the key
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public enum Permission {
        // Usage Permissions
        CREATE_LOCK("e2uclaim.lock", "You are not allowed to create locks!"),
        OVERRIDE_LOCK("e2uclaim.mod.lock", ""),
        CREATE_CLAIM("e2uclaim.claim", "You are not allowed to create claims!"),
        OVERRIDE_CLAIM("e2uclaim.mod.claim", ""),

        ADMIN("e2uclaim.admin"),

        // Numeric Permissions prefix
        CLAIM_SIZE("e2uclaim.claim.size.", "");

        @NotNull public final String node;
        @Nullable public final String customMissingMessage;

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
