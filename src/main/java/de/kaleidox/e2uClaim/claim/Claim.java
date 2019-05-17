package de.kaleidox.e2uClaim.claim;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.kaleidox.e2uClaim.E2UClaim;
import de.kaleidox.e2uClaim.interfaces.WorldLockable;
import de.kaleidox.e2uClaim.util.WorldUtil;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import static de.kaleidox.e2uClaim.util.WorldUtil.sort;

public class Claim implements WorldLockable {
    private final World world;
    private final UUID owner;
    private final int[][] area;
    // unused
    private @Nullable final int[] origin;
    private UUID[] member;

    public Claim(World world, UUID owner, UUID[] member, int[][] area, @Nullable int[] origin) {
        WorldUtil.expandVert(area);

        this.world = world;
        this.owner = owner;
        this.member = member;
        this.area = sort(area[0], area[1]);
        this.origin = origin;
    }

    public Claim(World world, UUID owner, int[][] area, @Nullable int[] origin) {
        this(world, owner, new UUID[0], area, origin);
    }

    public void save(ConfigurationSection config) {
        config.set("owner", owner.toString());

        List<String> members = config.getStringList("members");
        for (UUID uuid : member) members.add(uuid.toString());
        config.set("members", members);

        config.set("pos1.x", area[0][0]);
        config.set("pos1.y", area[0][1]);
        config.set("pos1.z", area[0][2]);

        config.set("pos2.x", area[1][0]);
        config.set("pos2.y", area[1][1]);
        config.set("pos2.z", area[1][2]);
    }

    public UUID getOwner() {
        return owner;
    }

    public World getWorld() {
        return world;
    }

    public int[][] getArea() {
        return area;
    }

    public Optional<int[]> getOrigin() {
        return Optional.ofNullable(origin);
    }

    @Override
    public <T extends CommandSender & Entity> boolean canAccess(T player) {
        if (E2UClaim.Permission.OVERRIDE_CLAIM.check(player)) return true;

        if (player.getUniqueId().equals(owner)) return true;
        for (UUID me : member) if (player.getUniqueId().equals(me)) return true;

        return false;
    }

    @Override
    public boolean isLocked(int[] xyz) {
        return WorldUtil.inside(area, xyz);
    }

    public boolean overlaps(int[][] check) { // FIXME: 12.05.2019 does not work
        int[][] areaC = sort(check[0], check[1]);
        int[][] areaM = sort(area[0], area[1]);

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

    public UUID[] getMembers() {
        return member;
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
}
