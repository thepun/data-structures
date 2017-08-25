package org.thepun.concurrency.queue;

class TestUtils {

    static long calcTotal(int n, int producers) {
        long val = 0;
        for (long l = 0; l < n * producers; l++) {
            val += l;
        }
        return val;
    }

}
