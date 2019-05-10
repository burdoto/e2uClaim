package de.kaleidox.e2uClaim.claim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.chat.MessageType;
import de.kaleidox.e2uClaim.exception.PluginEnableException;
import de.kaleidox.e2uClaim.interfaces.Initializable;
import de.kaleidox.e2uClaim.interfaces.Terminatable;
import de.kaleidox.e2uClaim.lock.Lock;
import de.kaleidox.e2uClaim.lock.LockManager;
import de.kaleidox.e2uClaim.util.BukkitUtil;
import de.kaleidox.e2uClaim.util.WorldUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static de.kaleidox.e2uClaim.E2UClaim.LOGGER;
import static de.kaleidox.e2uClaim.chat.Chat.message;
import static de.kaleidox.e2uClaim.util.ConfigurationUtil.getConfigSection;
import static de.kaleidox.e2uClaim.util.WorldUtil.xyz;

public enum ClaimManager implements Listener, Initializable, Terminatable {
    INSTANCE;

    private final Collection<Claim> claims = new ArrayList<>();
    private final Map<Player, int[]> awaitingClaim = new ConcurrentHashMap<>();

    public void requestClaiming(Player player) {
        awaitingClaim.compute(player, (k, v) -> new int[0]);
        message(player, MessageType.HINT, "Click the first corner to start claiming!");
    }

    public void requestUnclaiming(Player player) {
        if (awaitingClaim.remove(player) != null)
            message(player, MessageType.WARN, "Aborted previous %s command!", "/claim");
        int[] xyz = xyz(player.getLocation());

        Optional<Claim> any = claims.stream()
                .filter(claim -> claim.isLocked(xyz))
                .findAny();
        if (!any.isPresent()) {
            message(player, MessageType.WARN, "You do not stand in a claim. " +
                    "Please stand in the claim you want to remove.");
            return;
        }
        Claim claim = any.get();

        if (claim.canAccess(player) && !claim.getOwner().equals(player.getUniqueId())) {
            // only owner can remove
            message(player, MessageType.ERROR, "Only the owner of a claim can remove the claim!");
        } else if (claim.getOwner().equals(player.getUniqueId())) {
            // remove
            if (claims.remove(claim)) message(player, MessageType.INFO, "Claim removed!");
        } else {
            // you dont own this claim
            message(player, MessageType.ERROR, "You don't own this claim!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (world.getName().equals("configVersion")) return;

        Block targetBlock = event.getClickedBlock();
        if (targetBlock == null) return;
        int[] xyz = WorldUtil.xyz(targetBlock.getLocation());

        if (awaitingClaim.containsKey(player)
                && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            // any claim create hit
            int[] prevTarget = awaitingClaim.get(player);

            if (prevTarget.length == 0) {
                // first claim block
                if (claims.stream().noneMatch(claim -> claim.isLocked(xyz))) {
                    awaitingClaim.put(player, xyz);
                    message(player, MessageType.INFO, "First corner selected! " +
                            "Click on the second corner to create your claim!");
                } else message(player, MessageType.ERROR, "This block already is claimed!");
                event.setCancelled(true);
            } else {
                // second claim block
                if (claims.stream().noneMatch(claim -> claim.isLocked(xyz))) {
                    int[][] area = WorldUtil.sort(xyz, prevTarget);

                    // test for interfering locks
                    Set<Lock> interferingLocks = LockManager.INSTANCE.locks.stream()
                            .filter(lock -> !lock.canAccess(player))
                            .filter(lock -> lock.interferes(area))
                            .collect(Collectors.toSet());
                    if (interferingLocks.size() > 0) {
                        message(player, MessageType.ERROR, "This claim area interferes with %s locks!",
                                interferingLocks.size());
                        message(player, MessageType.WARN, "Claiming aborted.");
                        awaitingClaim.remove(player);
                        event.setCancelled(true);
                        return;
                    }

                    // test for interfering claims
                    Set<Claim> interferingClaims = claims.stream()
                            .filter(claim -> !claim.canAccess(player))
                            .filter(claim -> claim.interferes(area))
                            .collect(Collectors.toSet());
                    if (interferingClaims.size() > 0) {
                        message(player, MessageType.ERROR, "This claim area interferes with %s other claims!",
                                interferingClaims.size());
                        message(player, MessageType.WARN, "Claiming aborted.");
                        awaitingClaim.remove(player);
                        event.setCancelled(true);
                        return;
                    }

                    // check maximum allowed claim size
                    int newTotalClaimSize = (int) (claims.stream()
                            .filter(claim -> claim.getOwner().equals(player.getUniqueId()))
                            .mapToInt(claim -> (int) WorldUtil.dist(claim.getArea()[0], claim.getArea()[1]))
                            .sum() + WorldUtil.dist(area[0], area[1]));
                    int maxClaimSize = BukkitUtil.getNumericPermissionValue(E2UClaim.Permission.CLAIM_SIZE, player,
                            () -> E2UClaim.getConfig("config").getInt("defaults.claim-size"));
                    if (maxClaimSize < newTotalClaimSize) {
                        message(player, MessageType.ERROR, "Your maximum total claim side of %s was exceeded." +
                                " (New Size would be %s)", maxClaimSize, newTotalClaimSize);
                        message(player, MessageType.WARN, "Claiming aborted.");
                        awaitingClaim.remove(player);
                        event.setCancelled(true);
                        return;
                    }

                    Claim claim = new Claim(world, player.getUniqueId(), area, null);
                    claims.add(claim);
                    message(player, MessageType.INFO, "Your claim was created!");
                    awaitingClaim.remove(player);
                    event.setCancelled(true);
                }
            }
        } else {
            // protecc
            BlockFace face = event.getBlockFace();
            if (!E2UClaim.Permission.OVERRIDE_CLAIM.check(player))
                protecc(event.getPlayer(), new int[]{
                        xyz[0] + face.getModX(),
                        xyz[1] + face.getModY(),
                        xyz[2] + face.getModZ()
                }, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (E2UClaim.Permission.OVERRIDE_CLAIM.check(event.getPlayer())) return;
        if (event.getBlock().getWorld().getName().equals("configVersion")) return;
        final int[] xyz = xyz(event.getBlock().getLocation());

        protecc(event.getPlayer(), xyz, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (E2UClaim.Permission.OVERRIDE_CLAIM.check(event.getPlayer())) return;
        if (event.getBlock().getWorld().getName().equals("configVersion")) return;
        final int[] xyz = xyz(event.getBlock().getLocation());

        protecc(event.getPlayer(), xyz, event);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void init() {
        FileConfiguration claims = E2UClaim.getConfig("claims");

        switch (claims.getInt("configVersion", 1)) {
            default:
                throw new PluginEnableException("Unknown configuration version: " + claims.getInt("configVersion"));
            case 1:
                for (String worldName : claims.getKeys(false)) {
                    if (worldName.equals("configVersion")) continue;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        LOGGER.warning("Skipped loading claims for unknown world: " + worldName);
                        continue;
                    }
                    ConfigurationSection worldSection = getConfigSection(claims, worldName);
                    for (String claimName : worldSection.getKeys(false)) {
                        LOGGER.fine("Loading claim " + claimName + "...");
                        try {
                            this.claims.add(Claim.load(world, getConfigSection(worldSection, claimName)));
                        } catch (Exception e) {
                            LOGGER.severe("Error loading claim " + claimName + ": " + e.getMessage());
                        }
                    }
                }
        }

        LOGGER.info("Loaded " + this.claims.size() + " claim" + (this.claims.size() != 1 ? "s" : "") + "!");
    }

    @Override
    public void terminate() {
        int stored = 0;
        FileConfiguration claims = E2UClaim.getConfig("claims");
        claims.set("configVersion", 1);

        Map<String, List<Claim>> perWorldClaims = new HashMap<>();
        for (Claim me : this.claims) {
            String worldName = me.getWorld().getName();
            if (worldName.equals("configVersion")) continue;
            perWorldClaims.compute(worldName, (k, v) -> (v == null ? new ArrayList<>() : v)).add(me);
        }

        for (Map.Entry<String, List<Claim>> entry : perWorldClaims.entrySet()) {
            String world = entry.getKey();
            List<Claim> claim = entry.getValue();
            ConfigurationSection worldSection = claims.createSection(world);
            int c = 0;
            for (Claim me : claim) {
                assert worldSection != null;
                try {
                    me.save(worldSection.createSection("claim" + c++));
                    stored++;
                } catch (Exception e) {
                    LOGGER.severe("Error saving claim claim" + c + ": " + e.getMessage());
                }
            }
        }

        LOGGER.info("Saved " + stored + " claim" + (stored != 1 ? "s" : "") + "!");
    }

    private void protecc(Player player, int[] xyz, Cancellable event) {
        claims.parallelStream()
                .filter(claim -> claim.isLocked(xyz))
                .filter(claim -> !claim.canAccess(player)) //only failing locks
                .forEach(failedClaim -> {
                    event.setCancelled(true);
                    message(player, MessageType.WARN, "You cannot access this block!");
                });
    }
}
