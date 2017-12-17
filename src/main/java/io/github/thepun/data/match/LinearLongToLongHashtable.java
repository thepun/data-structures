package io.github.thepun.data.match;

import java.util.Arrays;

// TODO: change array access to unsafe
public final class LinearLongToLongHashtable implements LongToLongHashtable {

    private int size;
    private long[] data;

    public LinearLongToLongHashtable() {
        size = 10;
        data = new long[20];
        Arrays.fill(data, ELEMENT_NOT_FOUND);
    }

    @Override
    public long get(long key) {
        int index = calculateIndex(key);
        int firstIndex = index;

        long keyFromData;
        long valueFromData;
        for (;;) {
            keyFromData = data[index * 2];
            if (keyFromData == key) {
                valueFromData = data[index * 2 + 1];
                return valueFromData;
            }
            if (keyFromData == ELEMENT_NOT_FOUND) {
                return ELEMENT_NOT_FOUND;
            }

            index++;

            if (index == size) {
                index = 0;
            }

            if (index == firstIndex) {
                return ELEMENT_NOT_FOUND;
            }
        }
    }

    @Override
    public void set(long key, long value) {
        int index = calculateIndex(key);
        int firstIndex = index;

        long keyFromData;
        for (;;) {
            keyFromData = data[index * 2];
            if (keyFromData == ELEMENT_NOT_FOUND) {
                data[index * 2] = key;
                data[index * 2 + 1] = value;
                return;
            }

            if (keyFromData == key) {
                data[index * 2 + 1] = value;
                return;
            }

            index++;

            if (index == size) {
                index = 0;
            }

            if (index == firstIndex) {
                // increase table

                int newSize = size * 2;
                long[] newData = new long[newSize * 2];
                Arrays.fill(newData, ELEMENT_NOT_FOUND);

                long valueFromData;
                long anotherKeyFromData;
                for (int k = 0; k < size; k++) {
                    keyFromData = data[k * 2];
                    valueFromData = data[k * 2 + 1];

                    index = calculateIndex(keyFromData);
                    for (;;) {
                        anotherKeyFromData = newData[index * 2];
                        if (anotherKeyFromData == ELEMENT_NOT_FOUND) {
                            newData[index * 2] = keyFromData;
                            newData[index * 2 + 1] = valueFromData;
                            break;
                        }

                        index++;

                        if (index == newSize) {
                            index = 0;
                        }
                    }
                }

                data = newData;
                size = newSize;
                index = calculateIndex(key);
            }
        }
    }

    @Override
    public void remove(long key) {
        int index = calculateIndex(key);
        int firstIndex = index;

        long keyFromData;
        for (;;) {
            keyFromData = data[index * 2];
            if (keyFromData == key) {
                data[index * 2] = ELEMENT_NOT_FOUND;
                return;
            }
            if (keyFromData == ELEMENT_NOT_FOUND) {
                return;
            }

            index++;

            if (index == size) {
                index = 0;
            }

            if (index == firstIndex) {
                return;
            }
        }
    }

    private int calculateIndex(long key) {
        return (int) key % size;
    }
}
