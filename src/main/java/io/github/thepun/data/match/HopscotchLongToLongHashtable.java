package io.github.thepun.data.match;

import java.util.Arrays;

/**
 * Created by thepun on 01.10.17.
 */
public class HopscotchLongToLongHashtable {

    public static final long ELEMENT_NOT_FOUND = Long.MAX_VALUE;

    private static final int DEFAULT_CAPACITY = 16;
    private static final int SATURATION_INDEX = 5;
    private static final int SATURATION_DEGREE = 10;


    private int fill;
    private int capacity;
    private long[] data;

    public HopscotchLongToLongHashtable() {
        this(DEFAULT_CAPACITY);
    }

    public HopscotchLongToLongHashtable(int initialCapacity) {
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
        int indexKey = hash << 1;
        int firstIndexKey = indexKey;

        long anotherKey;

        // from initial to right edge
        for (;;) {
            anotherKey = localData[indexKey];
            if (anotherKey == key) {
                return localData[indexKey + 1];
            }

            indexKey += 2;

            if (indexKey == localCapacity) {
                break;
            }
        }

        // from left edge to initial
        indexKey = 0;
        for (;;) {
            if (indexKey == firstIndexKey) {
                return ELEMENT_NOT_FOUND;
            }

            anotherKey = localData[indexKey];
            if (anotherKey == key) {
                return localData[indexKey + 1];
            }

            indexKey += 2;
        }
    }

    private long readAndClear(long[] dataToUse, int dataCapacity, long key) {
        int hash = (int) (key % dataCapacity);
        int indexKey = hash << 1;
        int firstIndexKey = indexKey;

        long anotherKey;

        // from initial to right edge
        for (;;) {
            anotherKey = dataToUse[indexKey];
            if (anotherKey == key) {
                dataToUse[indexKey] = ELEMENT_NOT_FOUND;
                return dataToUse[indexKey + 1];
            }

            indexKey += 2;

            if (indexKey == dataCapacity) {
                break;
            }
        }

        // from left edge to initial
        indexKey = 0;
        for (;;) {
            if (indexKey == firstIndexKey) {
                return ELEMENT_NOT_FOUND;
            }

            anotherKey = dataToUse[indexKey];
            if (anotherKey == key) {
                dataToUse[indexKey] = ELEMENT_NOT_FOUND;
                return dataToUse[indexKey + 1];
            }

            indexKey += 2;
        }
    }

    private long write(long[] dataToPut, int dataCapacity, long key, long value) {
        int hash = (int) (key % dataCapacity);
        int indexKey = hash << 1;
        int firstIndexKey = indexKey;

        long anotherKey;

        // from initial to right edge
        for (;;) {
            anotherKey = dataToPut[indexKey];
            if (anotherKey == ELEMENT_NOT_FOUND) {
                dataToPut[indexKey] = key;
                dataToPut[indexKey + 1] = value;
                return ELEMENT_NOT_FOUND;
            } else if (anotherKey == key) {
                int indexValue = indexKey + 1;
                long anotherValue = dataToPut[indexValue];
                dataToPut[indexValue] = value;
                return anotherValue;
            }

            indexKey += 2;

            if (indexKey == dataCapacity) {
                break;
            }
        }

        // from left edge to initial
        indexKey = 0;
        for (;;) {
            if (indexKey == firstIndexKey) {
                return ELEMENT_NOT_FOUND;
            }

            anotherKey = dataToPut[indexKey];
            if (anotherKey == ELEMENT_NOT_FOUND) {
                dataToPut[indexKey] = key;
                dataToPut[indexKey + 1] = value;
                return ELEMENT_NOT_FOUND;
            } else if (anotherKey == key) {
                int indexValue = indexKey + 1;
                long anotherValue = dataToPut[indexValue];
                dataToPut[indexValue] = value;
                return anotherValue;
            }

            indexKey += 2;
        }
    }

    private void checkForEdge(long key) {
        if (key == ELEMENT_NOT_FOUND) {
            throw new IllegalStateException("Key " + key + " not allowed");
        }
    }
}
