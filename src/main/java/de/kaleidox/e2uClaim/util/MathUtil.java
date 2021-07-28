package de.kaleidox.e2uClaim.util;

import static java.lang.Math.abs;

@Deprecated
public final class MathUtil {
    private MathUtil() {
    }

    public static int mid(int x, int y) {
        return x / 2 + y / 2 + (x % 2 + y % 2) / 2;
    }

    public static int dist(int x, int y) {
        return abs(y - x);
    }

    public static boolean raising(int minimum, int middle, int maximum) {
        return minimum <= middle && middle <= maximum;
    }
}
