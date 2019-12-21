package de.kaleidox.e2uClaim.interfaces;

import org.bukkit.configuration.ConfigurationSection;

public interface Deserializable {
    void serializeToSection(ConfigurationSection section);
}
