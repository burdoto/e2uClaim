package de.kaleidox.e2uClaim.adapters.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.kaleidox.e2uClaim.adapters.CommandSendingAdapter;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class WorldEditAdapter implements WorldModificationAdapter {
    private static final Map<UUID, BukkitWorld> worldMap = new ConcurrentHashMap<>();
    public final WorldEditPlugin WORLDEDIT;
    private final WorldModificationAdapter fallbackAdapter;

    protected WorldEditAdapter(WorldEditPlugin worldedit, CommandSendingAdapter fallbackAdapter) {
        this.WORLDEDIT = worldedit;
        this.fallbackAdapter = fallbackAdapter;
    }

    public WorldModificationAdapter getFallbackAdapter() {
        return fallbackAdapter;
    }

    @Override
    public boolean setChestDisplayName(Player executor, Location location, String displayName) {
        final BukkitWorld world = getWorld(Objects.requireNonNull(location.getWorld(), "Invalid location!"));
        final BlockVector3 pos = loc2vec(location);
        final EditSession session = WORLDEDIT.createEditSession(executor);

        final BaseBlock block = session.getBlock(pos).toBaseBlock();
        final CompoundTag nbt = block.getNbtData();

        if (nbt != null) {
            nbt.setValue(new HashMap<String, Tag>(1) {{
                put("CustomName", simpleTag(displayName));
            }});

            block.setNbtData(nbt);

            try {
                session.setBlock(pos, block, EditSession.Stage.BEFORE_CHANGE);

                session.flushSession();
                return true;
            } catch (WorldEditException ignored) {
            }
        }

        return fallbackAdapter.setChestDisplayName(executor, location, displayName);
    }

    public static Tag simpleTag(Object value) {
        return new Tag() {
            private final Object val = value;

            @Override
            public Object getValue() {
                return val;
            }
        };
    }

    public static BukkitWorld getWorld(final World world) {
        return worldMap.computeIfAbsent(world.getUID(), uuid -> new BukkitWorld(world));
    }

    public static BlockVector3 loc2vec(Location location) {
        return BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
