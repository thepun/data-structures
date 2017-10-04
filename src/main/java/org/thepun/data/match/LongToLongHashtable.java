package org.thepun.data.match;

/**
 * Created by thepun on 01.10.17.
 */
public class LongToLongHashtable {

    public static final long ELEMENT_NOT_FOUND = Long.MAX_VALUE;


    private int size;
    private long[] data;

    public LongToLongHashtable() {
        size = 10;
        data = new long[20];
    }

    public long get(long key) {
        int localSize = size;
        long[] localData = data;
        int hash = (int) (key % localSize);



        return 0;
    }

    public void set(long key, long value) {
        if (value == ELEMENT_NOT_FOUND) {
            throw new IllegalStateException("Value " + value + " not allowed");
        }

        int localSize = size;
        long[] localData = data;
        int hash = (int) (key % localSize);


    }


}
