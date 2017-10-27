package io.github.thepun.data.utils;

public class PowOf2 {

    public static long roundUp(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value should be positive");
        }

        if (value > Long.MAX_VALUE / 2) {
            throw new IllegalStateException("Value is too big");
        }

        return (long) Math.pow(2, (int) Math.ceil(Math.log(value)/Math.log(2)));
    }

    public static int roundUp(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value should be positive");
        }

        if (value > Integer.MAX_VALUE / 2) {
            throw new IllegalStateException("Value is too big");
        }

        return (int) Math.pow(2, (int) Math.ceil(Math.log(value)/Math.log(2)));
    }


    private PowOf2() {
    }
}
