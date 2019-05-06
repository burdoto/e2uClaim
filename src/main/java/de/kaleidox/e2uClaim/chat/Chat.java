package de.kaleidox.e2uClaim.chat;


import de.kaleidox.e2uClaim.util.BukkitUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Chat {
    private Chat() {
    }

    public static void message(CommandSender sender, MessageType msgLevel, String format, Object... vars) {
        message(BukkitUtil.getPlayer(sender), msgLevel, format, vars);
    }

    public static void message(Player player, MessageType msgLevel, String format, Object... vars) {
        player.sendMessage(prefix() + msgLevel.chatColor
                + String.format(format, (Object[]) formatStrings(msgLevel, vars)));
    }

    public static void broadcast(MessageType msgLevel, String format, Object... vars) {
        Bukkit.broadcastMessage(prefix() + msgLevel.chatColor
                + String.format(format, (Object[]) formatStrings(msgLevel, vars)));
    }

    public static void broadcast(String permission, MessageType msgLevel, String format, Object... vars) {
        Bukkit.broadcast(prefix() + msgLevel.chatColor
                + String.format(format, (Object[]) formatStrings(msgLevel, vars)), permission);
    }

    private static String prefix() {
        return ChatColor.GRAY + "[" +
                ChatColor.GREEN + "e2uClaim" +
                ChatColor.GRAY + "] ";
    }

    private static String[] formatStrings(MessageType msgLevel, Object[] vars) {
        String[] strings = new String[vars.length];

        for (int i = 0; i < vars.length; i++)
            strings[i] = msgLevel.varColor + String.valueOf(vars[i]) + msgLevel.chatColor;

        return strings;
    }
}
