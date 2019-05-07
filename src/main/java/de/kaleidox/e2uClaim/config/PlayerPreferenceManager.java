package de.kaleidox.e2uClaim.config;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.interfaces.Initializable;
import de.kaleidox.e2uClaim.interfaces.Terminatable;

import org.bukkit.configuration.file.FileConfiguration;

public enum PlayerPreferenceManager implements Initializable, Terminatable {
    INSTANCE;

    private final Map<UUID, Map<String, String>> playerPropertyValueMap = new ConcurrentHashMap<>();
    private final Map<String, String> defaultValues = new ConcurrentHashMap<>();

    @Override
    public void init() throws IOException {
        FileConfiguration config = E2UClaim.getConfig("userPreferences");
        FileConfiguration defaults = E2UClaim.getConfig("config");
        Objects.requireNonNull(defaults.getConfigurationSection("defaults.preferences")).getKeys(false).forEach(property -> {
            String value = config.getString("defaults.preferences." + property);
            defaultValues.put(property, value);
        });
        Objects.requireNonNull(config.getKeys(false)).forEach(key -> {
            UUID uniqueId = UUID.fromString(key);
            Map<String, String> map = new ConcurrentHashMap<>();
            Objects.requireNonNull(config.getConfigurationSection(key)).getKeys(false).forEach(property -> {
                String value = config.getString(property);
                map.put(property, value);
            });
            playerPropertyValueMap.put(uniqueId, map);
        });
    }

    @Override
    public void terminate() throws IOException {
        FileConfiguration config = E2UClaim.getConfig("userPreferences");
        for (Map.Entry<UUID, Map<String, String>> entry : playerPropertyValueMap.entrySet()) {
            String uuid = entry.getKey().toString();
            Map<String, String> values = entry.getValue();
            values.forEach((key, value) -> E2UClaim.getConfig("userPreferences").set(uuid + "." + key, value));
        }
    }

    public String getProperty(UUID uuid, String property) {
        Map<String, String> properties = playerPropertyValueMap.getOrDefault(uuid, defaultValues);
        return properties.get(property);
    }

    public void setProperty(UUID uuid, String property, Object value) {
        String valueString = String.valueOf(value);
        Map<String, String> properties = playerPropertyValueMap.get(uuid);
        if (properties == null) {
            properties = new ConcurrentHashMap<>();
            properties.put(property, valueString);
            playerPropertyValueMap.put(uuid, properties);
        } else {
            if (properties.containsKey(property)) {
                properties.replace(property, valueString);
            } else {
                properties.put(property, valueString);
            }
            playerPropertyValueMap.replace(uuid, properties);
        }
    }
}
