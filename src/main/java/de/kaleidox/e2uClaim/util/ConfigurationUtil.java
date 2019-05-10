package de.kaleidox.e2uClaim.util;

import java.util.Objects;

import org.bukkit.configuration.ConfigurationSection;

public final class ConfigurationUtil {
    public static ConfigurationSection getConfigSection(ConfigurationSection from, String name) {
        if (from.isConfigurationSection(name))
            return Objects.requireNonNull(from.getConfigurationSection(name));
        return from.createSection(name);
    }
}
