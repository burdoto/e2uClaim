package de.kaleidox.e2uClaim.lock;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.interfaces.WorldLockable;
import de.kaleidox.e2uClaim.util.WorldUtil;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class Lock implements WorldLockable {
    private final World world;
    private final UUID owner;
    private final int[][] targets;
    private @Nullable final int[] origin;
    private LockConfiguration config;

    public Lock(World world, UUID owner, int[] target, @Nullable int[] origin) {
        this(world, owner, processTarget(world, target), origin);
    }

    private Lock(World world, UUID owner, int[][] targets, @Nullable int[] origin) {
        this.world = world;
        this.owner = owner;
        this.targets = targets;
        this.origin = origin;

        if (world == null)
            E2UClaim.LOGGER.warning("Suspicious lock loaded: world is null");
        if (targets.length == 0)
            E2UClaim.LOGGER.warning("Suspicious lock loaded: No targets defined");
        if (targets.length > 0 && (targets[0][0] == 0 && targets[0][1] == 0 && targets[0][2] == 0))
            E2UClaim.LOGGER.warning("Suspicious lock loaded: target[0] is " + Arrays.toString(targets[0]));
    }

    public World getWorld() {
        return world;
    }

    public Optional<int[]> getOrigin() {
        return Optional.ofNullable(origin);
    }

    public int[][] getAllMembers() {
        int[][] yields = new int[targets.length + (origin == null ? 0 : 1)][3];
        System.arraycopy(targets, 0, yields, 0, targets.length);
        if (origin != null) {
            yields[yields.length - 1][0] = origin[0];
            yields[yields.length - 1][1] = origin[1];
            yields[yields.length - 1][2] = origin[2];
        }
        return yields;
    }

    public int[] getMainTarget() {
        return targets[0];
    }

    public Material getMainTargetMaterial() {
        return world.getBlockAt(targets[0][0], targets[0][1], targets[0][2]).getType();
    }

    @Override
    public <T extends CommandSender & Entity> boolean canAccess(T player) {
        return E2UClaim.Permission.LOCK_OVERRIDE.check(player) || player.getUniqueId().equals(owner);
    }

    @Override
    public <T extends CommandSender & Entity> boolean tryAccess(T player, String pass) {
        return canAccess(player) && config.checkPassword(pass);
    }

    @Override
    public boolean isLocked(int[] xyz) {
        for (int[] locked : targets)
            if (Arrays.equals(locked, xyz))
                return true;
        return getOrigin().map(sign -> Arrays.equals(sign, xyz)).orElse(false);
    }

    public boolean interferes(Lock with) {
        for (int[] locked : with.targets)
            if (isLocked(locked))
                return true;
        return false;
    }

    public boolean interferes(int[][] area) {
        for (int[] test : targets)
            if (WorldUtil.inside(area, test))
                return true;
        return false;
    }

    public void save(ConfigurationSection config) {
        config.set("owner", owner.toString());

        for (int i = 0; i < targets.length; i++) {
            config.set("target." + i + ".x", targets[i][0]);
            config.set("target." + i + ".y", targets[i][1]);
            config.set("target." + i + ".z", targets[i][2]);
        }

        getOrigin().ifPresent(origin -> {
            config.set("origin.x", origin[0]);
            config.set("origin.y", origin[1]);
            config.set("origin.z", origin[2]);
        });
    }

    public static Lock load(World world, ConfigurationSection config) {
        UUID owner = UUID.fromString(Objects.requireNonNull(config.getString("owner")));

        ConfigurationSection targetSection = config.getConfigurationSection("target");
        assert targetSection != null;
        Set<String> keys = targetSection.getKeys(false);
        int[][] targets = new int[keys.size()][3];
        for (int i = 0; i < keys.size(); i++) {
            targets[i][0] = targetSection.getConfigurationSection(String.valueOf(i)).getInt("x");
            targets[i][1] = targetSection.getConfigurationSection(String.valueOf(i)).getInt("y");
            targets[i][2] = targetSection.getConfigurationSection(String.valueOf(i)).getInt("z");
        }

        ConfigurationSection originSection = config.getConfigurationSection("origin");
        int[] origin = null;
        if (originSection != null) {
            origin = new int[3];
            origin[0] = originSection.getInt("x");
            origin[1] = originSection.getInt("y");
            origin[2] = originSection.getInt("z");
        }

        return new Lock(world, owner, targets, origin);
    }

    private static int[][] processTarget(World world, int[] target) {
        Block targetBlock = world.getBlockAt(target[0], target[1], target[2]);
        BlockState targetBlockState = targetBlock.getState();
        Multiblock multiblock = Multiblock.getFromType(targetBlock.getType());

        switch (multiblock) {
            case DOOR:
                Block doorTarget = world.getBlockAt(target[0], target[1] + 1, target[2]);
                if (Multiblock.getFromType(doorTarget.getType()) == Multiblock.DOOR)
                    return new int[][]{target, new int[]{target[0], target[1] + 1, target[2]}};
                return new int[][]{target, new int[]{target[0], target[1] - 1, target[2]}};
            case CHEST:
                break;
        }

        return new int[][]{target};
    }

    public enum Multiblock {
        DOOR,
        CHEST,
        SINGLEBLOCK;

        public static Multiblock getFromType(Material material) {
            switch (material) {
                case JUNGLE_DOOR:
                case ACACIA_DOOR:
                case BIRCH_DOOR:
                case IRON_DOOR:
                case OAK_DOOR:
                case SPRUCE_DOOR:
                case DARK_OAK_DOOR:
                    return DOOR;
                case CHEST:
                case TRAPPED_CHEST:
                    return CHEST;
                default:
                    return SINGLEBLOCK;
            }
        }
    }
}
