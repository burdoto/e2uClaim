package de.kaleidox.e2uClaim.interfaces;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface WorldLockable {
    <T extends CommandSender & Entity> boolean canAccess(Player player);

    boolean isLocked(int[] xyz);
}
