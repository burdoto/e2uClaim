package de.kaleidox.e2uClaim.claim;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.interfaces.WorldLockable;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.comroid.spiroid.util.WorldUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.comroid.spiroid.util.WorldUtil.sort;

public class Claim extends CuboidRegion implements WorldLockable {
    private final UUID owner;
    private final int[][] bounds;
    // unused
    private @Nullable
    final int[] origin;
    private final UUID[] member;

    public UUID getOwner() {
        return owner;
    }

    public int[][] getBounds() {
        return bounds;
    }

    public Optional<int[]> getOrigin() {
        return Optional.ofNullable(origin);
    }

    public UUID[] getMembers() {
        return member;
    }

    public Claim(World world, UUID owner, UUID[] member, int[][] bounds, @Nullable int[] origin) {
        super(new BukkitWorld(world), blockVector(sort(bounds[0], bounds[1])[0]), blockVector(sort(bounds[0], bounds[1])[1]));
        WorldUtil.expandVert(bounds);

        this.owner = owner;
        this.member = member;
        this.bounds = sort(bounds[0], bounds[1]);
        this.origin = origin;
    }

    public Claim(World world, UUID owner, int[][] bounds, @Nullable int[] origin) {
        this(world, owner, new UUID[0], bounds, origin);
    }

    private static BlockVector3 blockVector(int[] xyz) {
        return BlockVector3.at(xyz[0], xyz[1], xyz[2]);
    }

    public static Claim load(World world, ConfigurationSection config) {
        UUID owner = UUID.fromString(Objects.requireNonNull(config.getString("owner")));

        List<String> members = config.getStringList("members");
        UUID[] uuids = new UUID[members.size()];
        for (int i = 0; i < members.size(); i++) uuids[i] = UUID.fromString(members.get(i));

        int[][] area = new int[2][3];

        area[0][0] = config.getInt("pos1.x");
        area[0][1] = config.getInt("pos1.y");
        area[0][2] = config.getInt("pos1.z");

        area[1][0] = config.getInt("pos2.x");
        area[1][1] = config.getInt("pos2.y");
        area[1][2] = config.getInt("pos2.z");

        return new Claim(world, owner, uuids, area, null);
    }

    public void save(ConfigurationSection config) {
        config.set("owner", owner.toString());

        List<String> members = config.getStringList("members");
        for (UUID uuid : member) members.add(uuid.toString());
        config.set("members", members);

        config.set("pos1.x", bounds[0][0]);
        config.set("pos1.y", bounds[0][1]);
        config.set("pos1.z", bounds[0][2]);

        config.set("pos2.x", bounds[1][0]);
        config.set("pos2.y", bounds[1][1]);
        config.set("pos2.z", bounds[1][2]);
    }

    @Override
    public <T extends CommandSender & Entity> boolean canAccess(T player) {
        if (E2UClaim.Permission.CLAIM_OVERRIDE.check(player)) return true;

        if (player.getUniqueId().equals(owner)) return true;
        for (UUID me : member) if (player.getUniqueId().equals(me)) return true;

        return false;
    }

    @Override
    public boolean isLocked(int[] xyz) {
        return WorldUtil.inside(bounds, xyz);
    }

    public boolean overlaps(int[][] check) { // FIXME: 12.05.2019 does not work
        int[][] areaC = sort(check[0], check[1]);
        int[][] areaM = sort(bounds[0], bounds[1]);

        int min_x1 = areaC[0][0];
        int min_x2 = areaM[0][0];
        int max_x1 = areaC[1][0];
        int max_x2 = areaM[1][0];
        int min_y1 = areaC[0][1];
        int min_y2 = areaM[0][1];
        int max_y1 = areaC[1][1];
        int max_y2 = areaM[1][1];
        int min_z1 = areaC[0][2];
        int min_z2 = areaM[0][2];
        int max_z1 = areaC[1][2];
        int max_z2 = areaM[1][2];

        return ((min_x1 <= min_x2 && min_x2 <= max_x1) || (min_x2 <= min_x1 && min_x1 <= max_x2)) &&
                ((min_y1 <= min_y2 && min_y2 <= max_y1) || (min_y2 <= min_y1 && min_y1 <= max_y2)) &&
                ((min_z1 <= min_z2 && min_z2 <= max_z1) || (min_z2 <= min_z1 && min_z1 <= max_z2));
    }
}
