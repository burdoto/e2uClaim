package de.kaleidox.e2uClaim.interfaces;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public interface WorldLockable {
    <T extends CommandSender & Entity> boolean canAccess(T player);

    boolean isLocked(int[] xyz);
}
