package org.sharpler.glag.util;

public final class TimeUtils {
    private TimeUtils() {
        // No-op.
    }

    public static boolean match(double xStart, double xFinish, double yStart, double yFinish) {
        if (xStart == yStart || xStart == yFinish || yFinish == yStart || yFinish == xFinish) {
            return true;
        }
        if (xStart < yStart) {
            return xFinish > yStart;
        } else {
            return xStart < yFinish;
        }
    }
}
