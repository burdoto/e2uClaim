package de.kaleidox.e2uClaim.command;

import java.util.ArrayList;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.chat.MessageType;

import org.bukkit.command.CommandSender;

import static de.kaleidox.e2uClaim.chat.Chat.message;

public enum SystemCommand implements Subcommand {
    INSTANCE;

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        switch (args.length) {
            case 0:
                message(sender, MessageType.HINT, "e2uClaim v" + E2UClaim.CONST.VERSION);
                return true;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "reload":
                    case "rl":
                        E2UClaim.INSTANCE.reloadConfig();
                        return true;
                }
        }

        return false;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void tabComplete(CommandSender sender, String alias, String[] args, ArrayList<String> list) {
        switch (args.length) {
            case 0:
                list.add("reload");
                break;
        }
    }
}
