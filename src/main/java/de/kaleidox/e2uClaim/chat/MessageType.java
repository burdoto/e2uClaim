package de.kaleidox.e2uClaim.chat;

import org.bukkit.ChatColor;

public enum MessageType {
    HINT(ChatColor.GREEN, ChatColor.AQUA),
    INFO(ChatColor.AQUA, ChatColor.GREEN),
    WARN(ChatColor.YELLOW, ChatColor.RED),
    ERROR(ChatColor.RED, ChatColor.LIGHT_PURPLE),
    EXCEPTION(ChatColor.DARK_RED, ChatColor.DARK_PURPLE);

    public final ChatColor chatColor;
    public final ChatColor varColor;

    MessageType(ChatColor chatColor, ChatColor varColor) {
        this.chatColor = chatColor;
        this.varColor = varColor;
    }
}
