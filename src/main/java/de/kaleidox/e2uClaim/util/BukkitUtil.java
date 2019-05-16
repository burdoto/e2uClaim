package de.kaleidox.e2uClaim.util;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import de.kaleidox.e2uClaim.E2UClaim;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

public final class BukkitUtil {
    private BukkitUtil() {
    }

    public static UUID getUuid(CommandSender cmdSender) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            if (onlinePlayer.getName().equals(cmdSender.getName()))
                return onlinePlayer.getUniqueId();
        throw new AssertionError("Sender is not online!");
    }

    public static Optional<Player> getPlayer(CommandSender cmdSender) {
        if (cmdSender instanceof Player) return Optional.of((Player) cmdSender);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equals(cmdSender.getName()))
                return Optional.of(onlinePlayer);
        }

        return Optional.empty();
    }

    public static Optional<Material> getMaterial(@Nullable String name) {
        if (name == null) return Optional.empty();

        Material val = Material.getMaterial(name);
        if (val != null) return Optional.of(val);

        for (Material value : Material.values())
            if (value.name().equalsIgnoreCase(name))
                return Optional.of(value);
        return Optional.empty();
    }

    public static int getNumericPermissionValue(
            E2UClaim.Permission permission,
            Permissible entity,
            Supplier<Integer> fallback
    ) {
        return entity.getEffectivePermissions()
                .stream()
                .filter(perm -> perm.getPermission().indexOf(permission.node) == 0)
                .findFirst()
                .map(PermissionAttachmentInfo::getPermission)
                .map(str -> {
                    String[] split = str.split("\\.");
                    return split[split.length - 1];
                })
                .map(Integer::parseInt)
                .orElseGet(fallback);
    }
}
