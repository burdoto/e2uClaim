package de.kaleidox.e2uClaim.lock;

import de.kaleidox.e2uClaim.interfaces.LockableConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class LockConfiguration implements LockableConfiguration {
    private final Lock lock;
    private @Nullable String password;

    public Lock getLock() {
        return lock;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public LockConfiguration(Lock lock, ConfigurationSection data) {
        this.lock = lock;

        final String password = data.getString("password", null);
        if (password == null) data.set("password", null);
    }

    @Override
    public boolean checkPassword(String password) {
        return Objects.nonNull(password) && getPassword()
                .map(password::equals)
                .orElse(true);
    }
}
