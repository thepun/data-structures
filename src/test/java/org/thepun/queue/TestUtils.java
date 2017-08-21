package org.thepun.queue;

public class TestUtils {

    public static long calcTotal(int n, int producers) {
        long val = 0;
        for (long l = 0; l < n * producers; l++) {
            val += l;
        }
        return val;
    }

}
