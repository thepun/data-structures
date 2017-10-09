package org.thepun.data.match;

import java.util.Arrays;

/**
 * Created by thepun on 01.10.17.
 */
public class LongToLongHashtable {

    public static final long ELEMENT_NOT_FOUND = Long.MAX_VALUE;

    private static final int DEFAULT_CAPACITY = 16;
    private static final int SATURATION_INDEX = 5;
    private static final int SATURATION_DEGREE = 10;


    private int fill;
    private int capacity;
    private long[] data;

    public LongToLongHashtable() {
        this(DEFAULT_CAPACITY);
    }

    public LongToLongHashtable(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity should be greater then zero");
        }

        fill = 0;
        capacity = initialCapacity;
        data = new long[initialCapacity * 2];
        Arrays.fill(data, ELEMENT_NOT_FOUND);
    }

    public int length() {
        return fill;
    }

    public long get(long key) {
        checkForEdge(key);

        long[] localData = data;
        int localCapacity = capacity;

        return read(localData, localCapacity, key);
    }

    public long set(long key, long value) {
        checkForEdge(key);

        long[] localData = data;
        int localFill = fill;
        int localCapacity = capacity;

        long result = write(localData, localCapacity, key, value);
        if (result == ELEMENT_NOT_FOUND) {
            localFill++;

            int fillIndex = localFill * SATURATION_DEGREE / localCapacity;
            if (fillIndex >= SATURATION_INDEX) {
                enlarge();
            }

            fill = localFill;
        }

        return result;
    }

    public long remove(long key) {
        checkForEdge(key);

        long[] localData = data;
        int localFill = fill;
        int localCapacity = capacity;

        long result = readAndClear(localData, localCapacity, key);
        if (result != ELEMENT_NOT_FOUND) {
            localFill--;

            int fillIndex = localFill * SATURATION_DEGREE / localCapacity;
            if (fillIndex < SATURATION_INDEX) {
                diminish();
            }

            fill = localFill;
        }

        return result;
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
                write(newData, newCapacity, key, value);
            }
        }

        return newData;
    }

    private long read(long[] localData, int localCapacity, long key) {
        int hash = (int) (key % localCapacity);

        long anotherKey;
        int indexKey, indexValue;
        for (;;) {
            indexKey = hash << 1;
            indexValue = (hash << 1) | 1;

            anotherKey = localData[indexKey];
            if (anotherKey == key) {
                return localData[indexValue];
            }

            hash = (hash + 1) % localCapacity;
        }
    }

    private long readAndClear(long[] localData, int localCapacity, long key) {
        int hash = (int) (key % localCapacity);

        long anotherKey;
        int indexKey, indexValue;
        for (;;) {
            indexKey = hash << 1;
            indexValue = (hash << 1) | 1;

            anotherKey = localData[indexKey];
            if (anotherKey == key) {
                localData[indexKey] = ELEMENT_NOT_FOUND;
                return localData[indexValue];
            }

            hash = (hash + 1) % localCapacity;
        }
    }

    private long write(long[] dataToPut, int dataCapacity, long key, long value) {
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
                return anotherKey;
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
