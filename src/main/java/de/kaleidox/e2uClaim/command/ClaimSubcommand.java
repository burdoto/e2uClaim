package de.kaleidox.e2uClaim.command;

import de.kaleidox.e2uClaim.claim.ClaimManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.comroid.spiroid.command.SpiroidCommand;
import org.comroid.spiroid.util.BukkitUtil;

import java.util.NoSuchElementException;
import java.util.stream.IntStream;

public enum ClaimSubcommand implements SpiroidCommand {
    list() {
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
    };

    private final SpiroidCommand[] subcommands;

    @Override
    public SpiroidCommand[] getSubcommands() {
        return subcommands;
    }

    ClaimSubcommand(SpiroidCommand... subcommands) {
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
