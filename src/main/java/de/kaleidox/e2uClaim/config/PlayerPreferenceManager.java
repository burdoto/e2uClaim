package de.kaleidox.e2uClaim.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    public void init() {
        FileConfiguration config = E2UClaim.getConfig("userPreferences");
        FileConfiguration defaults = E2UClaim.getConfig("config");
        defaultValues.put("autoLock", Boolean.toString(defaults.getBoolean("defaults.auto-lock")));
        defaultValues.put("toolItem", defaults.getString("defaults.tool-item"));
        for (String key : config.getKeys(false)) {
            UUID playerUniqueId = UUID.fromString(key);
            boolean autoLock;
            if (Boolean.valueOf(config.getBoolean(key + ".auto-lock")) != null) {
                autoLock = config.getBoolean(key + ".auto-lock");
            } else {
                autoLock = Boolean.valueOf(defaultValues.get("autoLock"));
            }
            String toolItem;
            if (config.getString(key + ".tool-item") != null) {
                toolItem = config.getString(key + ".tool-item");
            } else {
                toolItem = defaultValues.get("toolItem");
            }
            Map<String, String> map = new HashMap<>();
            map.put("autoLock", Boolean.toString(autoLock));
            map.put("toolItem", toolItem);
            playerPropertyValueMap.put(playerUniqueId, map);
        }
    }

    @Override
    public void terminate() throws IOException {
        FileConfiguration config = E2UClaim.getConfig("userPreferences");
        for (Map.Entry<UUID, Map<String, String>> entry : playerPropertyValueMap.entrySet()) {
            String uuid = entry.getKey().toString();
            Map<String, String> values = entry.getValue();
            boolean autoLock;
            if (values.containsKey("autoLock")) {
                autoLock = Boolean.valueOf(values.get("autoLock"));
            } else {
                autoLock = Boolean.valueOf(defaultValues.get("autoLock"));
            }
            String toolItem;
            if (values.containsKey("toolItem")) {
                toolItem = values.get("toolItem");
            } else {
                toolItem = defaultValues.get("toolItem");
            }
            config.set(uuid + ".auto-lock", autoLock);
            config.set(uuid + ".tool-item", toolItem);
            E2UClaim.set("userPreferences", config);
        }
    }

    public String getProperty(UUID uuid, String property) {
        Map<String, String> properties = playerPropertyValueMap.getOrDefault(uuid, defaultValues);
        return properties.get(property);
    }

    public void setProperty(UUID uuid, String property, String value) {
        Map<String, String> properties = playerPropertyValueMap.get(uuid);
        if (properties == null) {
            properties = new ConcurrentHashMap<>();
            properties.put(property, value);
            playerPropertyValueMap.put(uuid, properties);
        } else {
            if (properties.containsKey(property)) {
                properties.replace(property, value);
            } else {
                properties.put(property, value);
            }
            playerPropertyValueMap.replace(uuid, properties);
        }
    }
}
