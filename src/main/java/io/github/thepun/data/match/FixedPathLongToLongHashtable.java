/**
 * Copyright (C)2011 - Marat Gariev <thepun599@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thepun.data.match;

import java.util.Arrays;

import io.github.thepun.data.utils.PowOf2;
import io.github.thepun.unsafe.ArrayMemory;

/**
 * Created by thepun on 01.10.17.
 */
public class FixedPathLongToLongHashtable implements LongToLongHashtable {

    public static final long ELEMENT_NOT_FOUND = Long.MAX_VALUE;

    private static final int DEFAULT_CAPACITY = 16;
    private static final int SATURATION_LOW = 5;
    private static final int SATURATION_HIGH = 9;
    private static final int SATURATION_DEGREE = 10;
    private static final int GUARANTIED_AMOUNT = 16;
    private static final int DATA_SUFIX = 0x01;
    private static final int LINK_1_2_SUFIX = 0x02;
    private static final int LINK_3_4_SUFIX = 0x03;
    private static final int INDEX_STEP = 4;
    private static final int INDEX_STEP_SHIFT = 2;
    private static final int GUARANTIED_AMOUNT_INDEX_OFFSET = GUARANTIED_AMOUNT * INDEX_STEP;
    private static final int INT_SHIFT = 32;


    private int fill;
    private int capacity;
    private long[] data;

    public FixedPathLongToLongHashtable() {
        this(DEFAULT_CAPACITY);
    }

    public FixedPathLongToLongHashtable(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity should be greater then zero");
        }

        initialCapacity = PowOf2.roundUp(initialCapacity);

        fill = 0;
        capacity = initialCapacity;
        data = new long[initialCapacity << INDEX_STEP_SHIFT + GUARANTIED_AMOUNT];
        Arrays.fill(data, ELEMENT_NOT_FOUND);
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int length() {
        return fill;
    }

    @Override
    public long get(long key) {
        checkForEdge(key);

        long[] localData = data;
        int localCapacity = capacity;
        int hash = (int) (key % localCapacity);
        long indexKey = hash << INDEX_STEP_SHIFT;

        long anotherKey;

        // try current key
        anotherKey = ArrayMemory.getLong(localData, indexKey);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, indexKey | DATA_SUFIX);
        }

        // first and second links
        long link12 = ArrayMemory.getLong(localData, indexKey | LINK_1_2_SUFIX);
        int link1 = (int) (link12 >> INT_SHIFT);
        int link2 = (int) link12;

        // check first link
        if (link1 == 0) {
            return ELEMENT_NOT_FOUND;
        }

        // try first link
        anotherKey = ArrayMemory.getLong(localData, link1);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, link1 | DATA_SUFIX);
        }

        // check second link
        if (link2 == 0) {
            return ELEMENT_NOT_FOUND;
        }

        // try second link
        anotherKey = ArrayMemory.getLong(localData, link2);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, link2 | DATA_SUFIX);
        }

        // third and forth links
        long link34 = ArrayMemory.getLong(localData, indexKey | LINK_3_4_SUFIX);
        int link3 = (int) (link34 >> INT_SHIFT);
        int link4 = (int) link34;

        // check third link
        if (link3 == 0) {
            return ELEMENT_NOT_FOUND;
        }

        // try third link
        anotherKey = ArrayMemory.getLong(localData, link3);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, link3 | DATA_SUFIX);
        }

        // check forth link
        if (link4 == 0) {
            return ELEMENT_NOT_FOUND;
        }

        // try forth link
        anotherKey = ArrayMemory.getLong(localData, link4);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, link4 | DATA_SUFIX);
        }

        return ELEMENT_NOT_FOUND;
    }

    @Override
    public long set(long key, long value) {
        checkForEdge(key);

        long[] localData = data;
        int localFill = fill;
        int localCapacity = capacity;
        int hash = (int) (key % localCapacity);
        long indexKey = hash << INDEX_STEP_SHIFT;
        long result;

        long anotherKey;

        for (;;) {
            // try current key
            anotherKey = ArrayMemory.getLong(localData, indexKey);
            if (anotherKey == key) {
                break;
            }

            // first and second links
            long link12 = ArrayMemory.getLong(localData, indexKey | LINK_1_2_SUFIX);
            int link1 = (int) (link12 >> INT_SHIFT);
            int link2 = (int) link12;

            // check first link
            if (link1 == 0) {
                indexKey = link1;

                anotherKey = ArrayMemory.getLong(localData, link1);
                if (anotherKey == ELEMENT_NOT_FOUND) {

                }

                break;
            }

            // try first link
            anotherKey = ArrayMemory.getLong(localData, link1);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link1 | DATA_SUFIX);
            }

            // check second link
            if (link2 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            // try second link
            anotherKey = ArrayMemory.getLong(localData, link2);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link2 | DATA_SUFIX);
            }

            // third and forth links
            long link34 = ArrayMemory.getLong(localData, indexKey | LINK_3_4_SUFIX);
            int link3 = (int) (link34 >> INT_SHIFT);
            int link4 = (int) link34;

            // check third link
            if (link3 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            // try third link
            anotherKey = ArrayMemory.getLong(localData, link3);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link3 | DATA_SUFIX);
            }

            // check forth link
            if (link4 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            // try forth link
            anotherKey = ArrayMemory.getLong(localData, link4);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link4 | DATA_SUFIX);
            }
        }

        // set element to detected position
        result = ArrayMemory.getLong(localData, indexKey | DATA_SUFIX);
        ArrayMemory.setLong(localData, indexKey | DATA_SUFIX, value);


        // update counters because we added new element

        /*localFill++;

        int fillIndex = localFill * SATURATION_DEGREE / localCapacity;
        if (fillIndex >= SATURATION_HIGH) {
            enlarge();
        }

        fill = localFill;*/


        return result;
    }

    @Override
    public long remove(long key) {
        checkForEdge(key);

        long[] localData = data;
        int localFill = fill;
        int localCapacity = capacity;
        int hash = (int) (key % localCapacity);
        long indexKey = hash << INDEX_STEP_SHIFT;

        long result;
        long anotherKey;
        for (;;) {
            // try current key
            anotherKey = ArrayMemory.getLong(localData, indexKey);
            if (anotherKey == key) {
                result = ArrayMemory.getLong(localData, indexKey | DATA_SUFIX);
                ArrayMemory.setLong(localData, indexKey | DATA_SUFIX, ELEMENT_NOT_FOUND);
                break;
            }

            // first and second links
            long link12 = ArrayMemory.getLong(localData, indexKey | LINK_1_2_SUFIX);
            int link1 = (int) (link12 >> INT_SHIFT);
            int link2 = (int) link12;

            // check first link
            if (link1 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            // try first link
            anotherKey = ArrayMemory.getLong(localData, link1);
            if (anotherKey == key) {
                result = ArrayMemory.getLong(localData, link1 | DATA_SUFIX);
                ArrayMemory.setLong(localData, link1 | DATA_SUFIX, ELEMENT_NOT_FOUND);
                break;
            }

            // check second link
            if (link2 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            // try second link
            anotherKey = ArrayMemory.getLong(localData, link2);
            if (anotherKey == key) {
                result = ArrayMemory.getLong(localData, link2 | DATA_SUFIX);
                ArrayMemory.setLong(localData, link2 | DATA_SUFIX, ELEMENT_NOT_FOUND);
                break;
            }

            // third and forth links
            long link34 = ArrayMemory.getLong(localData, indexKey | LINK_3_4_SUFIX);
            int link3 = (int) (link34 >> INT_SHIFT);
            int link4 = (int) link34;

            // check third link
            if (link3 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            // try third link
            anotherKey = ArrayMemory.getLong(localData, link3);
            if (anotherKey == key) {
                result = ArrayMemory.getLong(localData, link3 | DATA_SUFIX);
                ArrayMemory.setLong(localData, link3 | DATA_SUFIX, ELEMENT_NOT_FOUND);
                break;
            }

            // check forth link
            if (link4 == 0) {
                return ELEMENT_NOT_FOUND;
            }

            anotherKey = ArrayMemory.getLong(localData, link4);
            if (anotherKey == key) {
                result = ArrayMemory.getLong(localData, link4 | DATA_SUFIX);
                ArrayMemory.setLong(localData, link4 | DATA_SUFIX, ELEMENT_NOT_FOUND);
                break;
            }
        }

        // if we found something update counters
        localFill--;
        int fillIndex = localFill * SATURATION_DEGREE / localCapacity;
        if (fillIndex < SATURATION_LOW) {
            diminish();
        }
        fill = localFill;
        return result;
    }

    private void enlarge() {
        int localCapacity = capacity;
        long[] localData = data;

        int newCapacity = localCapacity << 1;
        data = extendData(localData, localCapacity, newCapacity);
        capacity = newCapacity;
    }

    private void diminish() {
        int localCapacity = capacity;
        long[] localData = data;

        int newCapacity = localCapacity >> 1;
        data = extendData(localData, localCapacity, newCapacity);
        capacity = newCapacity;
    }

    private long[] extendData(long[] oldData, int oldCapacity, int newCapacity) {
        long[] newData = new long[newCapacity << 1 + GUARANTIED_AMOUNT];
        Arrays.fill(newData, ELEMENT_NOT_FOUND);

        long key, value;
        for (int i = 0; i < oldCapacity; i++) {
            key = oldData[i << 1];
            if (key != ELEMENT_NOT_FOUND) {
                value = oldData[(i << 1) | 1];
                int hash = (int) (key % newCapacity);
                int indexKey = hash << 1;

                long anotherKey;

                // from initial to right edge
                for (; ; ) {
                    anotherKey = newData[indexKey];
                    if (anotherKey == ELEMENT_NOT_FOUND) {
                        newData[indexKey] = key;
                        newData[indexKey + 1] = value;
                    } else if (anotherKey == key) {
                        int indexValue = indexKey + 1;
                        long anotherValue = newData[indexValue];
                        newData[indexValue] = value;
                    }

                    indexKey += 2;

                    if (indexKey == newCapacity) {
                        break;
                    }
                }
            }
        }

        return newData;
    }

    private static void checkForEdge(long key) {
        if (key == ELEMENT_NOT_FOUND) {
            throw new IllegalStateException("Key " + key + " not allowed");
        }
    }
}
