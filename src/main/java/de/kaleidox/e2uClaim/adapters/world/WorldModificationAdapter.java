package de.kaleidox.e2uClaim.adapters.world;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.adapters.CommandSendingAdapter;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public interface WorldModificationAdapter {
    boolean setChestDisplayName(Player executor, Location location, String displayName);

    static WorldModificationAdapter getInstance() {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        final CommandSendingAdapter fallbackAdapter = new CommandSendingAdapter();

        Plugin worldEdit;
        if ((worldEdit = pluginManager.getPlugin("WorldEdit")) != null)
            return new WorldEditAdapter((WorldEditPlugin) worldEdit, fallbackAdapter);

        E2UClaim.LOGGER.info("WorldEdit not found; using fallback WorldModificationAdapter: " + fallbackAdapter);

        return fallbackAdapter;
    }
}
