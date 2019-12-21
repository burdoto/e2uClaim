package de.kaleidox.e2uClaim.interfaces;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public interface WorldLockable {
    <T extends CommandSender & Entity> boolean canAccess(T player);

    <T extends CommandSender & Entity> boolean tryAccess(T player, String pass);

    boolean isLocked(int[] xyz);
}
