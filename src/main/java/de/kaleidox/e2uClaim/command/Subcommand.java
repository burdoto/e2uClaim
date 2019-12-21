package de.kaleidox.e2uClaim.command;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;

public interface Subcommand {
    boolean execute(CommandSender sender, String[] args);

    @Contract(mutates = "param4")
    void populateTabCompletion(CommandSender sender, String alias, String[] args, ArrayList<String> list);
}
