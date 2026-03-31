package org.sharpler.glag.util;

public final class TimeUtils {
    private TimeUtils() {
        // No-op.
    }

    public static boolean match(double xStart, double xFinish, double yStart, double yFinish) {
        return xStart == yStart
            || xStart == yFinish
            || yStart == yFinish
            || yFinish == xFinish
            || (xStart < yFinish && yStart < xFinish);
    }
}
