package de.kaleidox.e2uClaim.command;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.chat.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.comroid.spiroid.api.command.SpiroidCommand;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static de.kaleidox.e2uClaim.chat.Chat.message;

public enum SystemSubcommand implements SpiroidCommand {
    reload(){
        @Override
        public String execute(CommandSender sender, String[] args) {
            E2UClaim.instance.reloadConfig();
            return "Reload Complete!";
        }
    },
    exclude(){
        @Override
        public String[] tabComplete(String startsWith) {
            return Bukkit.getWorlds()
                    .stream()
                    .map(World::getName)
                    .toArray(String[]::new);
        }

        @Override
        public String execute(CommandSender sender, String[] args) {
            FileConfiguration config = E2UClaim.instance.getConfig();
            List<String> excluded = config.getStringList("excluded-worlds");

            String world = args[0];
            if (Bukkit.getWorld(world) != null) {
                if (excluded.contains(world)) {
                    excluded.remove(world);
                    message(sender, MessageType.INFO, "World %s is no longer excluded from %s!",
                            world, "e2uClaim");
                } else {
                    excluded.add(world);
                    message(sender, MessageType.INFO, "World %s is now excluded from %s!",
                            world, "e2uClaim");
                }
            } else {
                if (excluded.remove(world))
                    message(sender, MessageType.HINT, "Unknown World %s was removed from exclusion!", world);
                else message(sender, MessageType.ERROR, "World %s does not exist!", world);
            }

            config.set("excluded-worlds", excluded);
            return "Successfully excluded world: " + world;
        }
    };

    @Override
    public SpiroidCommand[] getSubcommands() {
        return new SpiroidCommand[0];
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
