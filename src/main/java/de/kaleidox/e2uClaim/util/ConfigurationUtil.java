package de.kaleidox.e2uClaim.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public final class ConfigurationUtil {
    public static ConfigurationSection getConfigSection(ConfigurationSection from, String name) {
        if (from.isConfigurationSection(name))
            return Objects.requireNonNull(from.getConfigurationSection(name));
        return from.createSection(name);
    }
}
