package de.kaleidox.e2uClaim.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static de.kaleidox.e2uClaim.util.MathUtil.raising;

public final class WorldUtil {
    private WorldUtil() {
    }

    public static double dist(int[] pos1, int[] pos2) {
        return sqrt(pow(pos2[0] - pos1[0], 2) + pow(pos2[2] - pos1[2], 2));
    }

    public static int[] mid(int[][] pos) {
        return new int[]{
                MathUtil.mid(pos[0][0], pos[1][0]),
                MathUtil.mid(pos[0][1], pos[1][1]),
                MathUtil.mid(pos[0][2], pos[1][2])
        };
    }

    public static boolean inside(int[][] area, int[] xyz) {
        return raising(min(area[0][0], area[1][0]), xyz[0], max(area[0][0], area[1][0]))
                && raising(min(area[0][1], area[1][2]), xyz[1], max(area[0][1], area[1][2]))
                && raising(min(area[0][2], area[1][2]), xyz[2], max(area[0][2], area[1][2]));
    }

    public static int[] xyz(Location location) {
        return new int[]{location.getBlockX(), location.getBlockY(), location.getBlockZ()};
    }

    public static Location location(World world, int[] xyz) {
        return world.getBlockAt(xyz[0], xyz[1], xyz[2]).getLocation();
    }

    @Contract(mutates = "param1")
    public static int[][] expandVert(int[][] positions) {
        positions[0][1] = 0;
        positions[1][1] = 256;
        return positions;
    }

    public static int[][] retract(int[][] pos, int retractBy) {
        return new int[][]{
                new int[]{min(pos[0][0], pos[1][0]) + retractBy, pos[0][1], min(pos[0][2], pos[1][2]) + retractBy},
                new int[]{max(pos[0][0], pos[1][0]) - retractBy, pos[0][1], max(pos[0][2], pos[1][2]) - retractBy}
        };
    }

    public static int[][] sort(int[] pos1, int[] pos2) {
        return new int[][]{
                new int[]{min(pos1[0], pos2[0]), min(pos1[1], pos2[1]), min(pos1[2], pos2[2])},
                new int[]{max(pos1[0], pos2[0]), max(pos1[1], pos2[1]), max(pos1[2], pos2[2])}
        };
    }

    public static void breakDependent(Player player, Block block) {
        switch (player.getGameMode()) {
            case CREATIVE:
            case SPECTATOR:
                block.setType(Material.AIR);
                break;
            case SURVIVAL:
            case ADVENTURE:
                block.breakNaturally();
                break;
        }
    }

    @MagicConstant(valuesFromClass = ChestState.class)
    public static int chestState(Block block) {
        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory) {
                DoubleChest doubleChest = (DoubleChest) inventory.getHolder();
                return ChestState.DOUBLE_CHEST;
            }
            return ChestState.SIMPLE_CHEST;
        }
        return ChestState.NO_CHEST;
    }

    public final class ChestState {
        public static final int NO_CHEST = 0;
        public static final int SIMPLE_CHEST = 1;
        public static final int DOUBLE_CHEST = 2;
    }
}
