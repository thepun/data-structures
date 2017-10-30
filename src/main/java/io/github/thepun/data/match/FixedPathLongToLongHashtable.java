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

    private static final int DEFAULT_CAPACITY = 16;
    private static final int SATURATION_LOW = 5;
    private static final int SATURATION_HIGH = 9;
    private static final int SATURATION_DEGREE = 10;
    private static final int GUARANTIED_AMOUNT = 16;
    private static final int DATA_SUFIX = 0x01;
    private static final long LINK_X_X_MASK = 0x0000000000000000L;
    private static final long LINK_1_X_MASK = 0xFFFFFFFF00000000L;
    private static final long LINK_X_2_MASK = 0xFFFFFFFF;
    private static final long LINK_3_X_MASK = 0xFFFFFFFF00000000L;
    private static final int LINK_1_2_SUFIX = 0x02;
    private static final int LINK_3_4_SUFIX = 0x03;
    private static final int INDEX_STEP = 4;
    private static final int INDEX_STEP_SHIFT = 2;
    private static final int GUARANTIED_AMOUNT_INDEX_OFFSET = GUARANTIED_AMOUNT * INDEX_STEP;
    private static final int INT_SHIFT = 32;


    //private int fill;
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

        //fill = 0;
        capacity = initialCapacity;
        data = new long[initialCapacity << INDEX_STEP_SHIFT + GUARANTIED_AMOUNT];
        Arrays.fill(data, ELEMENT_NOT_FOUND);
    }

    /*@Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int length() {
        return fill;
    }*/

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

        // fetch links
        long link12 = ArrayMemory.getLong(localData, indexKey | LINK_1_2_SUFIX);
        long link34 = ArrayMemory.getLong(localData, indexKey | LINK_3_4_SUFIX);
        int link1 = (int) (link12 >> INT_SHIFT);
        int link2 = (int) link12;
        int link3 = (int) (link34 >> INT_SHIFT);

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

        // check third link
        if (link3 == 0) {
            return ELEMENT_NOT_FOUND;
        }

        // try third link
        anotherKey = ArrayMemory.getLong(localData, link3);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, link3 | DATA_SUFIX);
        }

        return ELEMENT_NOT_FOUND;
    }

    @Override
    public void set(long key, long value) {
        checkForEdge(key);

        long[] localData = data;
        int localCapacity = capacity;

        // vars
        int link1, link2, link3, link4, allowedDisplace;
        long indexKey, anotherKey, anotherValue, link12, link34, offset12, offset34, link12Mask, link34Mask, nextIndexKey;

        insert:
        for (;;) {
            link12Mask = LINK_X_X_MASK;
            link34Mask = LINK_X_X_MASK;

            // find link to insert
            find:
            for (; ; ) {
                int hash = (int) (key % localCapacity);
                indexKey = hash << INDEX_STEP_SHIFT;
                nextIndexKey = indexKey;

                // try current key
                anotherKey = ArrayMemory.getLong(localData, indexKey);

                // if current element is empty
                if (anotherKey == ELEMENT_NOT_FOUND) {
                    break find;
                }

                // if current element has the same key
                if (anotherKey == key) {
                    link12Mask = LINK_X_X_MASK;
                    link34Mask = LINK_X_X_MASK;
                    break find;
                }

                // load links
                offset12 = indexKey | LINK_1_2_SUFIX;
                offset34 = indexKey | LINK_3_4_SUFIX;
                link12 = ArrayMemory.getLong(localData, offset12);
                link34 = ArrayMemory.getLong(localData, offset34);
                link1 = (int) (link12 >> INT_SHIFT);
                link2 = (int) link12;
                link3 = (int) (link34 >> INT_SHIFT);

                // check first link
                if (link1 == 0) {
                    indexKey = ELEMENT_NOT_FOUND;
                    link12Mask = LINK_1_X_MASK;
                    link34Mask = LINK_X_X_MASK;
                    break find;
                }

                // try first link
                anotherKey = ArrayMemory.getLong(localData, link1);
                if (anotherKey == key) {
                    indexKey = link1;
                    break find;
                }

                // check second link
                if (link2 == 0) {
                    indexKey = ELEMENT_NOT_FOUND;
                    link12Mask = LINK_X_2_MASK;
                    link34Mask = LINK_X_X_MASK;
                    break find;
                }

                // try second link
                anotherKey = ArrayMemory.getLong(localData, link2);
                if (anotherKey == key) {
                    indexKey = link2;
                    break find;
                }

                // check third link
                if (link3 == 0) {
                    indexKey = ELEMENT_NOT_FOUND;
                    link12Mask = LINK_X_X_MASK;
                    link34Mask = LINK_3_X_MASK;
                    break find;
                }

                // try third link
                anotherKey = ArrayMemory.getLong(localData, link3);
                if (anotherKey == key) {
                    indexKey = link3;
                    break find;
                }

                // we don't have empty elements in the bucket -> increase table
                enlarge();
                localData = data;
                localCapacity = capacity;
            }

            // find place to insert if we don't know one
            if (indexKey == ELEMENT_NOT_FOUND) {
                indexKey = nextIndexKey;

                allowedDisplace = GUARANTIED_AMOUNT;
                for (; ; ) {
                    indexKey += INDEX_STEP;
                    offset34 = indexKey | LINK_3_4_SUFIX;
                    allowedDisplace--;

                    link4 = (int) ArrayMemory.getLong(localData, offset34);
                    if (link4 == 0) {
                        // switch key and value
                        anotherKey = ArrayMemory.getLong(localData, indexKey);
                        anotherValue = ArrayMemory.getLong(localData, indexKey | DATA_SUFIX);
                        ArrayMemory.setLong(localData, indexKey, key);
                        ArrayMemory.setLong(localData, indexKey | DATA_SUFIX, value);

                        // update links
                        offset12 = indexKey | LINK_1_2_SUFIX;
                        link34 = ArrayMemory.getLong(localData, offset34);
                        link12 = ArrayMemory.getLong(localData, offset12);
                        link34 = ((indexKey << INDEX_STEP_SHIFT | indexKey) & link34Mask) | link34;
                        link12 = ((indexKey << INDEX_STEP_SHIFT | indexKey) & link12Mask) | link12;
                        ArrayMemory.setLong(localData, offset34, link34);
                        ArrayMemory.setLong(localData, offset12, link12);

                        // do set with new key and value
                        key = anotherKey;
                        value = anotherValue;
                        continue insert;
                    }

                    if (allowedDisplace == 0) {
                        enlarge();
                        localData = data;
                        localCapacity = capacity;
                        continue insert;
                    }
                }
            }

            // set element to detected position
            ArrayMemory.setLong(localData, indexKey | DATA_SUFIX, value);

            break insert;
        }
    }

    @Override
    public void remove(long key) {
        checkForEdge(key);

        long[] localData = data;
        //int localFill = fill;
        int localCapacity = capacity;
        int hash = (int) (key % localCapacity);
        long indexKey = hash << INDEX_STEP_SHIFT;

        // try current key
        long anotherKey = ArrayMemory.getLong(localData, indexKey);
        if (anotherKey == key) {
            ArrayMemory.setLong(localData, indexKey | DATA_SUFIX, ELEMENT_NOT_FOUND);
            return;
        }

        // first and second links
        long link12 = ArrayMemory.getLong(localData, indexKey | LINK_1_2_SUFIX);
        long link34 = ArrayMemory.getLong(localData, indexKey | LINK_3_4_SUFIX);
        int link1 = (int) (link12 >> INT_SHIFT);
        int link2 = (int) link12;
        int link3 = (int) (link34 >> INT_SHIFT);

        // check first link
        if (link1 != 0) {
            // try first link
            anotherKey = ArrayMemory.getLong(localData, link1);
            if (anotherKey == key) {
                ArrayMemory.setLong(localData, link1 | DATA_SUFIX, ELEMENT_NOT_FOUND);
                return;
            }
        }

        // check second link
        if (link2 != 0) {
            // try second link
            anotherKey = ArrayMemory.getLong(localData, link2);
            if (anotherKey == key) {
                ArrayMemory.setLong(localData, link2 | DATA_SUFIX, ELEMENT_NOT_FOUND);
                return;
            }
        }

        // check third link
        if (link3 != 0) {
            // try third link
            anotherKey = ArrayMemory.getLong(localData, link3);
            if (anotherKey == key) {
                ArrayMemory.setLong(localData, link3 | DATA_SUFIX, ELEMENT_NOT_FOUND);
            }
        }
    }

    private void enlarge() {
        int localCapacity = capacity;
        long[] localData = data;

        int newCapacity = localCapacity << 1;
        long[] newData = new long[newCapacity << INDEX_STEP_SHIFT + GUARANTIED_AMOUNT];
        Arrays.fill(newData, ELEMENT_NOT_FOUND);

        long key, value;
        for (int i = 0; i < localCapacity; i++) {
            key = localData[i << 1];
            if (key != ELEMENT_NOT_FOUND) {
                int indexKey = i << INDEX_STEP_SHIFT;
                value = ArrayMemory.getLong(localData, indexKey | DATA_SUFIX);

            }
        }

        data = newData;
        capacity = newCapacity;
    }

    /*private void diminish() {
        int localCapacity = capacity;
        long[] localData = data;

        int newCapacity = localCapacity >> 1;
        long[] newData = new long[newCapacity << 1 + GUARANTIED_AMOUNT];
        Arrays.fill(newData, ELEMENT_NOT_FOUND);

        long key, value;
        for (int i = 0; i < localCapacity; i++) {
            key = localData[i << 1];
            if (key != ELEMENT_NOT_FOUND) {
                value = localData[(i << 1) | 1];
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

        data = newData;
        capacity = newCapacity;
    }*/

    private static void checkForEdge(long key) {
        if (key == ELEMENT_NOT_FOUND) {
            throw new IllegalStateException("Key " + key + " not allowed");
        }
    }
}
