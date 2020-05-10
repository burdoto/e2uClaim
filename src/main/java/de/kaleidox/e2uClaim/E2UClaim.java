package de.kaleidox.e2uClaim;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.kaleidox.e2uClaim.chat.Chat;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.claim.ClaimManager;
import de.kaleidox.e2uClaim.command.ClaimCommand;
import de.kaleidox.e2uClaim.command.SystemCommand;
import de.kaleidox.e2uClaim.exception.PluginEnableException;
import de.kaleidox.e2uClaim.interfaces.Initializable;
import de.kaleidox.e2uClaim.lock.LockManager;
import de.kaleidox.e2uClaim.util.BukkitUtil;
import de.kaleidox.e2uClaim.util.WorldUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.comroid.spiroid.api.AbstractPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class E2UClaim extends JavaPlugin {
    public static final String PATH_BASE = "plugins/e2uClaim/";
    private static final Initializable[] initializables;
    private static final Closeable[] closeables;
    public static E2UClaim INSTANCE;
    public static Logger LOGGER;
    public static Const CONST;
    private static Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();

    static {
        initializables = new Initializable[]{
                ClaimManager.INSTANCE,
                LockManager.INSTANCE
        };

        closeables = new Closeable[]{
                ClaimManager.INSTANCE,
                LockManager.INSTANCE
        };
    }

    @Override
    public void reloadConfig() {
        configs.forEach((name, config) -> {
            try {
                File file = new File(PATH_BASE + name + ".yml");
                config.load(name);
            } catch (InvalidConfigurationException | IOException e) {
                LOGGER.severe("Error reloading config " + name + ": " + e.getMessage());
            }
        });
    }

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
        if (playerOptional.map(WorldUtil::isExcludedWorld)
                .orElse(false)) {
            Chat.message(sender, MessageType.WARN, "You are in an excluded world!");
            return false;
        }

        switch (label.toLowerCase()) {
            case "e2uclaim":
            case "e2uc":
                return SystemCommand.INSTANCE.execute(sender, args);
            case "lock":
                playerOptional.ifPresent(LockManager.INSTANCE::requestLock);
                return true;
            case "unlock":
                playerOptional.ifPresent(LockManager.INSTANCE::requestUnlock);
                return true;
            case "claim":
                if (args.length > 0) ClaimCommand.INSTANCE.execute(sender, args);
                else playerOptional.ifPresent(ClaimManager.INSTANCE::requestClaiming);
                return true;
            case "unclaim":
                playerOptional.ifPresent(ClaimManager.INSTANCE::requestUnclaiming);
                return true;
        }

        return super.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        ArrayList<String> yields = new ArrayList<>();

        switch (alias.toLowerCase()) {
            case "e2uclaim":
            case "e2uc":
                SystemCommand.INSTANCE.populateTabCompletion(sender, alias, args, yields);
                break;
            case "claim":
                ClaimCommand.INSTANCE.populateTabCompletion(sender, alias, args, yields);
                break;
        }

        yields.removeIf(str -> str.indexOf(args[args.length - 1]) != 0);
        return yields;
    }

    @Override
    public void onLoad() {
        INSTANCE = this;
        LOGGER = getLogger();

        super.onLoad();

        FileConfiguration config = getConfig("config");
        if (!config.isSet("defaults.claim-size"))
            config.set("defaults.claim-size", 128);
        if (!config.isSet("excluded-worlds"))
            config.set("excluded-worlds", new ArrayList<>());
        cycle();

        saveDefaultConfig();
        configs.put("config", getConfig("config"));
    }

    @Override
    public void onDisable() {
        super.onDisable();

        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.severe("Error closing " + closeable.toString() + ": " + e.getMessage());
            }
        }

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
            CONST = new Const();
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

            for (Initializable initializable : initializables) {
                try {
                    initializable.init();
                } catch (IOException e) {
                    LOGGER.severe("Error initializing " + initializable.toString() + ": " + e.getMessage());
                }
            }

            String excluded = getConfig("config")
                    .getStringList("excluded-worlds")
                    .stream()
                    .map(str -> {
                        if (Bukkit.getWorld(str) == null)
                            return str + " [invalid world name]";
                        return str;
                    })
                    .collect(Collectors.joining(", "));
            if (!excluded.isEmpty())
                LOGGER.info("Excluded worlds: " + excluded);

            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::cycle,
                    BukkitUtil.time2tick(10, TimeUnit.SECONDS),
                    BukkitUtil.time2tick(5, TimeUnit.MINUTES));
        } catch (PluginEnableException e) {
            LOGGER.severe("Unable to load " + toString() + ": " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void cycle() {
        LOGGER.fine("Running plugin cycle...");

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
        final File dir = new File(PATH_BASE);
        if (!dir.exists())
            if (dir.mkdir())
                LOGGER.fine("Created configuration Directory");

        return configs.compute(name, (k, v) -> { // .compute will place the result of the BiFunction inside the map at the given key.
            if (v != null) return v; // if the value is already set, return it

            try { // if there is no value; create it:
                File file = new File(PATH_BASE + name + ".yml"); // get the file
                file.createNewFile(); // create the file if neccessary

                YamlConfiguration configuration = new YamlConfiguration(); // create the configuration
                configuration.load(file); // load the configuration
                return configuration; // place the configuration inside the map at the key
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException("Configuration: " + name, e);
            }
        });
    }

    public final static class Const {
        public final String VERSION;

        private Const() {
            try {
                YamlConfiguration yml = new YamlConfiguration();
                yml.load(new InputStreamReader(
                        Objects.requireNonNull(Const.class.getClassLoader().getResourceAsStream("plugin.yml"),
                                "Could not access plugin.yml")));

                VERSION = yml.getString("version");
            } catch (IOException | InvalidConfigurationException e) {
                throw new AssertionError("Unexpected Exception", e);
            }
        }
    }

    public enum Permission {
        // Usage Permissions
        LOCK_USE("e2uclaim.lock", "You are not allowed to create locks!"),
        LOCK_OVERRIDE("e2uclaim.mod.lock", ""),
        CLAIM_USE("e2uclaim.claim", "You are not allowed to create claims!"),
        CLAIM_OVERRIDE("e2uclaim.mod.claim", ""),

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

        public boolean check(CommandSender user, String customMissingMessage) {
            if (user.hasPermission(node)) return true;
            if (customMissingMessage == null)
                Chat.message(user, MessageType.ERROR, "You are missing the required permission: %s", node);
            else if (customMissingMessage.isEmpty()) return false;
            else Chat.message(user, MessageType.ERROR, customMissingMessage);
            return false;
        }
    }
}
