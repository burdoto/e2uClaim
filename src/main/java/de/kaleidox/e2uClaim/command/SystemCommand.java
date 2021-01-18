package de.kaleidox.e2uClaim.command;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.util.BukkitUtil;
import de.kaleidox.e2uClaim.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.kaleidox.e2uClaim.chat.Chat.message;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public enum SystemCommand implements Subcommand {
    INSTANCE;

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!E2UClaim.Permission.ADMIN.check(sender)) return false;

        switch (args.length) {
            case 0:
                BukkitUtil.getPlayer(sender)
                        .flatMap(plr -> {
                            final List<Block> lineOfSight = plr.getLineOfSight(null, 50);
                            final Optional<Block> block = lineOfSight
                                    .stream()
                                    .filter(b -> b.getType() != Material.AIR)
                                    .findAny()
                                    .flatMap(WorldUtil::doubleChest$otherSide);
                            return block;
                        })
                        .ifPresent(System.out::println);

                message(sender, MessageType.HINT, "e2uClaim v" + E2UClaim.instance.version);
                return true;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "reload":
                    case "rl":
                        E2UClaim.instance.reloadConfig();
                        return true;
                }
            case 2:
                switch (args[0].toLowerCase()) {
                    case "exclude":
                        FileConfiguration config = E2UClaim.instance.getConfig();
                        List<String> excluded = config.getStringList("excluded-worlds");

                        if (Bukkit.getWorld(args[1]) != null) {
                            if (excluded.contains(args[1])) {
                                excluded.remove(args[1]);
                                message(sender, MessageType.INFO, "World %s is no longer excluded from %s!",
                                        args[1], "e2uClaim");
                            } else {
                                excluded.add(args[1]);
                                message(sender, MessageType.INFO, "World %s is now excluded from %s!",
                                        args[1], "e2uClaim");
                            }
                        } else {
                            if (excluded.remove(args[1]))
                                message(sender, MessageType.HINT, "Unknown World %s was removed from exclusion!",
                                        args[1]);
                            else message(sender, MessageType.ERROR, "World %s does not exist!", args[1]);
                        }

                        config.set("excluded-worlds", excluded);
                        break;
                }
        }

        return false;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void populateTabCompletion(CommandSender sender, String alias, String[] args, ArrayList<String> list) {
        if (!E2UClaim.Permission.ADMIN.check(sender, "")) return;

        switch (args.length) {
            case 0:
            case 1:
                list.add("reload");
                list.add("exclude");
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "exclude":
                        for (World world : Bukkit.getWorlds()) list.add(world.getName());
                        list.addAll(E2UClaim.instance.getConfig().getStringList("excluded-worlds"));
                        break;
                }
                break;
        }
    }
}
