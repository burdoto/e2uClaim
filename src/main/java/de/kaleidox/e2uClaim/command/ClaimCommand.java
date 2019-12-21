package de.kaleidox.e2uClaim.command;

import java.util.ArrayList;
import java.util.Optional;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.claim.ClaimManager;
import de.kaleidox.e2uClaim.util.BukkitUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public enum ClaimCommand implements Subcommand {
    INSTANCE;

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Optional<Player> playerOptional = BukkitUtil.getPlayer(sender);
        if (!playerOptional.isPresent()) return false;
        Player player = playerOptional.get();

        switch (args.length) {
            case 0:
                return false;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "list":
                        ClaimManager.INSTANCE.listClaims(player, 0);
                        return true;
                }
            case 2:
                switch (args[0].toLowerCase()) {
                    case "list":
                        if (args[1].matches("0-9+")) {
                            int page = Integer.parseInt(args[1]);
                            ClaimManager.INSTANCE.listClaims(player, 1);
                            return true;
                        } else return false;
                }
        }
        return false;
    }

    @Override
    public void populateTabCompletion(CommandSender sender, String alias, String[] args, ArrayList<String> list) {
        if (!E2UClaim.Permission.CLAIM_USE.check(sender, "")) return;

        switch (args.length) {
            case 0:
            case 1:
                list.add("list");
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "list":
                        list.add("<int>");
                        break;
                }
                break;
        }
    }
}
