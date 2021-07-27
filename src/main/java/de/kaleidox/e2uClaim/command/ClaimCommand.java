package de.kaleidox.e2uClaim.command;

import de.kaleidox.e2uClaim.claim.ClaimManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.comroid.spiroid.api.command.SpiroidCommand;
import org.comroid.spiroid.api.util.BukkitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.stream.IntStream;

public enum ClaimCommand implements SpiroidCommand {
    LIST("list") {
        @Override
        public String execute(CommandSender sender, String[] args) {
            Player player = BukkitUtil.getPlayer(sender).orElseThrow(NoSuchElementException::new);
            int page = args.length == 0 ? 0 : Integer.parseInt(args[0]);
            ClaimManager.INSTANCE.listClaims(player, page);
            return "";
        }

        @Override
        public String[] tabComplete(String startsWith) {
            return IntStream.range(1, ClaimManager.INSTANCE.getPageCount())
                    .mapToObj(String::valueOf)
                    .toArray(String[]::new);
        }
    },
    INSTANCE("claim", LIST) {
    };

    private final String name;
    private final SpiroidCommand[] subcommands;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SpiroidCommand[] getSubcommands() {
        return subcommands;
    }

    ClaimCommand(String name, SpiroidCommand... subcommands) {
        this.name = name;
        this.subcommands = subcommands;
    }

    @Override
    public String[] tabComplete(String startsWith) {
        return new String[0];
    }

    @Override
    public String execute(CommandSender sender, String[] args) {
        return null;
    }
}
