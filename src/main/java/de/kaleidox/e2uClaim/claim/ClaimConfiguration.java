package de.kaleidox.e2uClaim.claim;

import java.util.Objects;
import java.util.Optional;

import de.kaleidox.e2uClaim.interfaces.LockableConfiguration;
import de.kaleidox.e2uClaim.lock.Lock;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public class ClaimConfiguration implements LockableConfiguration {
    private final Claim claim;
    private @Nullable String password;

    public ClaimConfiguration(Claim claim, ConfigurationSection data) {
        this.claim = claim;

        final String password = data.getString("password", null);
        if (password == null) data.set("password", null);
    }

    public Claim getClaim() {
        return claim;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    @Override
    public boolean checkPassword(String password) {
        return Objects.nonNull(password) && getPassword()
                .map(password::equals)
                .orElse(true);
    }
}
