package de.kaleidox.e2uClaim.adapters;

import de.kaleidox.e2uClaim.adapters.world.WorldModificationAdapter;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

// default fallback adapter
public class CommandSendingAdapter implements WorldModificationAdapter {
    @Override
    public boolean setChestDisplayName(Player executor, Location location, String displayName) {
        return runCommand(String.format("/data merge block %d %d %d {CustomName:\"%s\"}",
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), displayName));
    }

    public static boolean runCommand(String command) {
        if (!command.startsWith("/"))
            command = '/' + command;

        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
