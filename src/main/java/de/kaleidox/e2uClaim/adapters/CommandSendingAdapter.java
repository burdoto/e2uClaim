package de.kaleidox.e2uClaim.adapters;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.adapters.world.WorldModificationAdapter;
import de.kaleidox.e2uClaim.util.WorldUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

// default fallback adapter
public class CommandSendingAdapter implements WorldModificationAdapter {
    @Override
    public boolean setChestDisplayName(Player executor, Location location, String displayName) {
        if (WorldUtil.chestState(location.getBlock()) == WorldUtil.ChestState.NO_CHEST)
            throw new IllegalStateException("Block at " + location + " is not a chest!");

        return runCommand(String.format("/data merge block %d %d %d {CustomName:\"%s\"}",
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), displayName));
    }

    public static boolean runCommand(String command) {
        if (!command.startsWith("/"))
            command = '/' + command;

        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
