package net.wurstclient.util;

import java.util.Random;

public class Rand {
    private static final Random random = new Random();

    public static int Int(int min, int max) {
        if (min == max)
            return min;

        if (min > max) {
            min = min + max;
            max = min - max;
            min = min - max;
        }

        return random.nextInt(max - min + 1) + min;
    }
}