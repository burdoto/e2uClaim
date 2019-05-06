package de.kaleidox.e2uClaim.lock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.chat.Chat;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.exception.PluginEnableException;
import de.kaleidox.e2uClaim.util.WorldUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static de.kaleidox.e2uClaim.chat.Chat.message;
import static de.kaleidox.e2uClaim.util.WorldUtil.breakDependent;
import static de.kaleidox.e2uClaim.util.WorldUtil.xyz;

public enum LockManager implements Listener {
    INSTANCE;

    private final Collection<Lock> locks = new ArrayList<>();
    private final Collection<Player> awaitingLock = new ArrayList<>();
    private final Collection<Player> awaitingUnlock = new ArrayList<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().getWorld().getName().equals("configVersion")) return;
        String[] lines = event.getLines();
        if (lines[0] != null && !lines[0].equalsIgnoreCase("[lock]")) return;
        if (!E2UClaim.Permission.CREATE_LOCK.check(event.getPlayer())) {
            breakDependent(event.getPlayer(), event.getBlock());
            return;
        }
        if (!(event.getBlock().getBlockData() instanceof WallSign)) {
            message(event.getPlayer(), MessageType.ERROR,
                    "Lock Signs need to be placed on the side of the block that you want to lock.");
            breakDependent(event.getPlayer(), event.getBlock());
            return;
        }

        Player player = event.getPlayer();
        Block signBlock = event.getBlock();
        WallSign sign = (WallSign) signBlock.getBlockData();
        Sign signState = (Sign) signBlock.getState();
        int[] target;

        switch (sign.getFacing()) {
            case NORTH:
                target = xyz(signBlock.getLocation().add(0, 0, 1));
                break;
            case EAST:
                target = xyz(signBlock.getLocation().add(-1, 0, 0));
                break;
            case SOUTH:
                target = xyz(signBlock.getLocation().add(0, 0, -1));
                break;
            case WEST:
                target = xyz(signBlock.getLocation().add(1, 0, 0));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + sign.getFacing());
        }

        Block targetBlock = signBlock.getWorld().getBlockAt(target[0], target[1], target[2]);
        Lock lock = new Lock(signBlock.getWorld(), player.getUniqueId(), target, xyz(signBlock.getLocation()));
        for (Lock iter : locks)
            if (lock.interferes(iter)) {
                message(player, MessageType.ERROR, "This lock interferes with an existing lock!");
                breakDependent(player, signBlock);
                return;
            }
        locks.add(lock);
        event.setLine(0, "§8[§3Lock§8]");
        message(player, MessageType.INFO, "Lock created for %s at %s.",
                targetBlock.getType(), Arrays.toString(target));
        if (WorldUtil.chestState(targetBlock) == WorldUtil.ChestState.DOUBLE_CHEST)
            message(player, MessageType.WARN, "Warning: Multiblock-Chest locking is currently not supported." +
                    " Please lock both sides of the chest with one sign each.");
    }

    public void requestLock(Player player) {
        if (awaitingUnlock.remove(player))
            Chat.message(player, MessageType.WARN, "Aborted previous %s command!", "/unlock");
        awaitingLock.add(player);
        message(player, MessageType.HINT, "Click a block to lock it!");
    }

    public void requestUnlock(Player player) {
        if (awaitingLock.remove(player))
            Chat.message(player, MessageType.WARN, "Aborted previous %s command!", "/lock");
        awaitingUnlock.add(player);
        message(player, MessageType.HINT, "Click a block to unlock it!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getPlayer().getWorld().getName().equals("configVersion")) return;

        Block targetBlock = event.getClickedBlock();
        if (targetBlock == null) return;
        int[] xyz = xyz(targetBlock.getLocation());
        Player player = event.getPlayer();

        if (awaitingLock.remove(player)
                && (event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            // lock command creation instead
            Lock newLock = new Lock(targetBlock.getWorld(), player.getUniqueId(), xyz, null);
            if (locks.stream().noneMatch(newLock::interferes)) {
                locks.add(newLock);
                message(player, MessageType.INFO, "Lock created for %s at %s.",
                        targetBlock.getType(), Arrays.toString(newLock.getMainTarget()));
                event.setCancelled(true);
                if (WorldUtil.chestState(targetBlock) == WorldUtil.ChestState.DOUBLE_CHEST)
                    message(player, MessageType.WARN, "Warning: Multiblock-Chest locking is currently not" +
                            " supported. Please lock both sides of the chest seperately.");
            } else {
                event.setCancelled(true);
                message(player, MessageType.ERROR, "This lock interferes with an existing lock!");
            }
        } else if (awaitingUnlock.remove(player)
                && (event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            // unlocklock command creation instead
            Collection<Lock> toBeRemoved = locks.stream()
                    .filter(lock -> lock.isLocked(xyz))
                    .filter(lock -> lock.canAccess(player))
                    .collect(Collectors.toList());
            if (toBeRemoved.size() > 1)
                E2UClaim.LOGGER.warning("Suspicious unlock action: More than 1 lock found!");
            for (Lock oldLock : toBeRemoved) {
                if (locks.remove(oldLock)) {
                    oldLock.getOrigin()
                            .map(coord -> oldLock.getWorld().getBlockAt(coord[0], coord[1], coord[2]))
                            .ifPresent(block -> breakDependent(player, block));

                    message(event.getPlayer(), MessageType.INFO, "Removed lock from block %s at %s.",
                            oldLock.getMainTargetMaterial(), Arrays.toString(oldLock.getMainTarget()));
                    event.setCancelled(true);
                }
            }
        } else {
            locks.stream()
                    .filter(lock -> lock.isLocked(xyz))
                    .filter(lock -> !lock.canAccess(player)) //only failing locks
                    .forEach(failedLock -> {
                        event.setCancelled(true);
                        message(player, MessageType.WARN, "You cannot access this block!");
                    });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld().getName().equals("configVersion")) return;
        final int[] xyz = xyz(event.getBlock().getLocation());
        final Collection<Lock> removeLocks = new ArrayList<>();

        locks.parallelStream()
                .filter(lock -> lock.isLocked(xyz))
                .peek(lock -> {
                    for (int[] coord : lock.getAllMembers())
                        if (Arrays.equals(xyz, coord) && lock.canAccess(event.getPlayer())) removeLocks.add(lock);
                })
                //.filter(Objects::nonNull)
                .filter(lock -> !lock.canAccess(event.getPlayer())) //only failing locks
                .forEach(failedLock -> {
                    event.setCancelled(true);
                    message(event.getPlayer(), MessageType.WARN, "You cannot access this block!");
                });
        removeLocks.forEach(lock -> {
            if (locks.remove(lock))
                message(event.getPlayer(), MessageType.INFO, "Removed lock from block %s at %s.",
                        lock.getMainTargetMaterial(), Arrays.toString(lock.getMainTarget()));
        });
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public void load() {
        FileConfiguration locks = E2UClaim.getConfig("locks");

        switch (locks.getInt("configVersion", 1)) {
            default:
                throw new PluginEnableException("Unknown configuration version: " + locks.getInt("configVersion"));
            case 1:
                Set<String> worlds = locks.getKeys(false);
                worlds.forEach(worldName -> {
                    if (worldName.equals("configVersion")) return;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) return;
                    Set<String> thisWorld = Objects.requireNonNull(locks
                            .getConfigurationSection(worldName)).getKeys(false);
                    thisWorld.forEach(lockNr -> this.locks.add(Lock.load(world, Objects.requireNonNull(
                            Objects.requireNonNull(locks.getConfigurationSection(worldName))
                                    .getConfigurationSection(lockNr)))));
                });
                break;
        }

        E2UClaim.LOGGER.info("Loaded " + this.locks.size() + " lock" + (this.locks.size() != 1 ? "s" : "") + "!");
    }

    public void store() {
        FileConfiguration locks = E2UClaim.getConfig("locks");
        locks.set("configVersion", 1);

        Map<String, List<Lock>> perWorldLocks = new HashMap<>();
        this.locks.forEach(lock -> {
            String worldName = lock.getWorld().getName();
            if (worldName.equals("configVersion")) return;
            perWorldLocks.compute(worldName, (k, v) -> (v == null ? new ArrayList<>() : v)).add(lock);
        });

        perWorldLocks.forEach((world, lock) -> {
            ConfigurationSection configurationSection = locks.createSection(world);
            int c = 0;
            for (Lock l : lock) {
                assert configurationSection != null;
                l.save(configurationSection.createSection(String.valueOf(c++)));
            }
        });
    }
}
