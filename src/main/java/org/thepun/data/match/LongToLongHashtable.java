package org.thepun.data.match;

import java.util.Arrays;

/**
 * Created by thepun on 01.10.17.
 */
public class LongToLongHashtable {

    public static final long ELEMENT_NOT_FOUND = Long.MAX_VALUE;

    private static final int DEFAULT_CAPACITY = 16;


    private int fill;
    private int capacity;
    private long[] data;

    public LongToLongHashtable() {
        fill = 0;
        capacity = DEFAULT_CAPACITY;
        data = new long[DEFAULT_CAPACITY * 2];
    }

    public long get(long key) {
        checkForEdge(key);

        long[] localData = data;
        int localCapacity = capacity;
        int hash = (int) (key % localCapacity);



        return 0;
    }

    public void set(long key, long value) {
        checkForEdge(key);

        long[] localData = data;
        int localFill = fill;
        int localCapacity = capacity;
        int fillIndex = localFill * 10 / localCapacity;

        if (fillIndex >= 5) {
            enlarge();
            localCapacity = capacity;
        }

        put(localData, localCapacity, key, value);
    }

    public long remove(long key) {
        checkForEdge(key);


    }

    private void enlarge() {
        int localCapacity = capacity;
        long[] localData = data;

        int newCapacity = localCapacity << 1;
        data = copyData(localData, localCapacity, newCapacity);
        capacity = newCapacity;
    }

    private void diminish() {
        int localCapacity = capacity;
        long[] localData = data;

        int newCapacity = localCapacity >> 1;
        data = copyData(localData, localCapacity, newCapacity);
        capacity = newCapacity;
    }

    private long[] copyData(long[] oldData, int oldCapacity, int newCapacity) {
        long[] newData = new long[newCapacity << 1];
        Arrays.fill(newData, ELEMENT_NOT_FOUND);

        long key, value;
        for (int i = 0; i < oldCapacity; i++) {
            key = oldData[i << 1];
            if (key != ELEMENT_NOT_FOUND) {
                value = oldData[(i << 1) | 1];
                put(newData, newCapacity, key, value);
            }
        }

        return newData;
    }

    private void put(long[] dataToPut, int dataCapacity, long key, long value) {
        int hash = (int) (key % dataCapacity);

        long anotherKey;
        int indexKey, indexValue;
        for (;;) {
            indexKey = hash << 1;
            indexValue = (hash << 1) | 1;

            anotherKey = dataToPut[indexKey];
            if (anotherKey == ELEMENT_NOT_FOUND) {
                dataToPut[indexKey] = key;
                dataToPut[indexValue] = value;
                break;
            }

            hash = (hash + 1) % dataCapacity;
        }
    }

    private void checkForEdge(long key) {
        if (key == ELEMENT_NOT_FOUND) {
            throw new IllegalStateException("Key " + key + " not allowed");
        }
    }
}
