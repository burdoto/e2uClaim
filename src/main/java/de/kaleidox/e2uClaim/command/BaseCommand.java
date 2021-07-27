package de.kaleidox.e2uClaim.command;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.claim.ClaimManager;
import de.kaleidox.e2uClaim.lock.LockManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.comroid.spiroid.command.SpiroidCommand;
import org.comroid.spiroid.util.BukkitUtil;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public enum BaseCommand implements SpiroidCommand {
    claim(ClaimSubcommand.values()) {
        @Override
        public @Nullable String execute(CommandSender sender, String[] args) {
            Player player = BukkitUtil.getPlayer(sender).orElseThrow(NoSuchElementException::new);
            ClaimManager.INSTANCE.requestClaiming(player);
            return null;
        }
    },
    unclaim(ClaimSubcommand.values()) {
        @Override
        public @Nullable String execute(CommandSender sender, String[] args) {
            Player player = BukkitUtil.getPlayer(sender).orElseThrow(NoSuchElementException::new);
            ClaimManager.INSTANCE.requestUnclaiming(player);
            return null;
        }
    },
    lock() {
        @Override
        public @Nullable String execute(CommandSender sender, String[] args) {
            Player player = BukkitUtil.getPlayer(sender).orElseThrow(NoSuchElementException::new);
            LockManager.INSTANCE.requestLock(player);
            return null;
        }
    },
    unlock() {
        @Override
        public @Nullable String execute(CommandSender sender, String[] args) {
            Player player = BukkitUtil.getPlayer(sender).orElseThrow(NoSuchElementException::new);
            LockManager.INSTANCE.requestUnlock(player);
            return null;
        }
    },
    e2uClaim(SystemSubcommand.values()) {
        @Override
        public String execute(CommandSender sender, String[] args) {
            return "e2uClaim v" + E2UClaim.instance.version;
        }
    };

    private final SpiroidCommand[] subcommands;

    @Override
    public SpiroidCommand[] getSubcommands() {
        return subcommands;
    }

    BaseCommand(SpiroidCommand... subcommands) {
        this.subcommands = subcommands;
    }

    @Override
    public String[] tabComplete(String startsWith) {
        return new String[0];
    }

    @Override
    public @Nullable String execute(CommandSender sender, String[] args) {
        return null;
    }
}
