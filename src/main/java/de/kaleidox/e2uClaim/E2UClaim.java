package de.kaleidox.e2uClaim;

import de.kaleidox.e2uClaim.chat.Chat;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.command.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;
import org.comroid.spiroid.AbstractPlugin;
import org.comroid.spiroid.annotation.MinecraftPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

@MinecraftPlugin(dependencies = "WorldGuard")
public final class E2UClaim extends AbstractPlugin {
    public E2UClaim() {
        super(BaseCommand.values());


    }

    @Override
    public void load() {
        FileConfiguration config = Objects.requireNonNull(getConfig("config"), "main config");

        if (!config.isSet("defaults.claim-size"))
            config.set("defaults.claim-size", 128);
        if (!config.isSet("excluded-worlds"))
            config.set("excluded-worlds", new ArrayList<>());

        saveDefaultConfig();
        configs.put("config", getConfig("config"));
    }

    @Override
    public void enable() {
        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldEdit != null && worldGuard != null) {
            getLogger().info("Detected WorldEdit and WorldGuard! Forcing WorldEdit and WorldGuard enabling...");
            worldEdit.getPluginLoader().enablePlugin(worldEdit);
            worldGuard.getPluginLoader().enablePlugin(worldGuard);
            getLogger().info("Finished loading WorldEdit and WorldGuard. Removing WorldGuard SignChangeEvent listener...");
            SignChangeEvent.getHandlerList().unregister(worldGuard);
            getLogger().info("Disabled WorldGuard SignChangeEvent listener! Continuing enabling...");
        }

        World configVersion = Bukkit.getWorld("configVersion");
        if (configVersion != null)
            getLogger().warning("World with name \"configVersion\" detected. This world will be ignored by e2uClaim.");

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
            getLogger().info("Excluded worlds: " + excluded);
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

        @NotNull
        public final String node;
        @Nullable
        public final String customMissingMessage;

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
}
