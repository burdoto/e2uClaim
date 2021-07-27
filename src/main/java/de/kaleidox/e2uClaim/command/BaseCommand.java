package de.kaleidox.e2uClaim.command;

import de.kaleidox.e2uClaim.E2UClaim;
import org.bukkit.command.CommandSender;
import org.comroid.spiroid.command.SpiroidCommand;
import org.jetbrains.annotations.Nullable;

public enum BaseCommand implements SpiroidCommand {
    claim(ClaimSubcommand.values()),
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
