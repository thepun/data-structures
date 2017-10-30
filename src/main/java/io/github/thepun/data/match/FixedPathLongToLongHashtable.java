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
    private static final long LINK_X_X_MASK = 0x0000000000000000L;
    private static final long LINK_1_X_MASK = 0xFFFFFFFF00000000L;
    private static final long LINK_X_2_MASK = 0xFFFFFFFF;
    private static final long LINK_3_X_MASK = 0xFFFFFFFF00000000L;
    private static final int DATA_SUFIX = 0x10;
    private static final int LINK_1_2_SUFIX = 0x20;
    private static final int LINK_3_4_SUFIX = 0x30;
    private static final int INDEX_STEP = 4;
    private static final int INDEX_STEP_SHIFT = 2;
    private static final int INDEX_OFFSET_SHIFT = 5;
    private static final int GUARANTIED_AMOUNT_INDEX_OFFSET = GUARANTIED_AMOUNT * INDEX_STEP;
    private static final int INT_SHIFT = 32;
    private static final long INT_MASK = 0x00000000FFFFFFFFL;
    private static final long ARRAY_OFFSET = ArrayMemory.firstElementOffset();


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
        data = new long[(initialCapacity << INDEX_STEP_SHIFT) + GUARANTIED_AMOUNT_INDEX_OFFSET];
        Arrays.fill(data, ELEMENT_NOT_FOUND);
    }

    @Override
    public long get(long key) {
        checkForEdge(key);

        long anotherKey, link12, link34, link1, link2, link3, index;

        int localCapacity = capacity;
        long[] localData = data;
        long hash = key % localCapacity;
        index = ARRAY_OFFSET;
        hash = hash << INDEX_OFFSET_SHIFT;
        index = index + hash;

        // try current key
        anotherKey = ArrayMemory.getLong(localData, index);
        if (anotherKey == key) {
            return ArrayMemory.getLong(localData, index | DATA_SUFIX);
        }

        // fetch links
        link12 = index | LINK_1_2_SUFIX;
        link34 = index | LINK_3_4_SUFIX;
        link12 = ArrayMemory.getLong(localData, link12);
        link34 = ArrayMemory.getLong(localData, link34);
        link1 = link12 >> INT_SHIFT;
        link2 = link12 & INT_MASK;
        link3 = link34 >> INT_SHIFT;
        link1 = link1 << INDEX_OFFSET_SHIFT;
        link2 = link2 << INDEX_OFFSET_SHIFT;
        link3 = link3 << INDEX_OFFSET_SHIFT;
        link1 = link1 + ARRAY_OFFSET;
        link2 = link2 + ARRAY_OFFSET;
        link3 = link3 + ARRAY_OFFSET;

        // check first link
        if (link1 != 0) {
            // try first link
            anotherKey = ArrayMemory.getLong(localData, link1);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link1 | DATA_SUFIX);
            }
        }

        // check second link
        if (link2 != 0) {
            // try second link
            anotherKey = ArrayMemory.getLong(localData, link2);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link2 | DATA_SUFIX);
            }
        }

        // check third link
        if (link3 != 0) {
            // try third link
            anotherKey = ArrayMemory.getLong(localData, link3);
            if (anotherKey == key) {
                return ArrayMemory.getLong(localData, link3 | DATA_SUFIX);
            }
        }

        return ELEMENT_NOT_FOUND;
    }

    @Override
    public void set(long key, long value) {
        checkForEdge(key);

        long[] localData = data;
        int localCapacity = capacity;

        // vars
        int allowedDisplace;
        long index, indexWithArrayOffset, anotherKey, anotherValue, link12, link34, link1, link2, link3, link4,
                link12Mask, link34Mask, nextIndexKey, temp1, temp2, temp3, temp4, temp5;

        insert:
        for (;;) {
            link12Mask = LINK_X_X_MASK;
            link34Mask = LINK_X_X_MASK;

            // find link to insert
            find:
            for (; ; ) {
                index = key % localCapacity;
                indexWithArrayOffset = ARRAY_OFFSET;
                index = index << INDEX_OFFSET_SHIFT;
                indexWithArrayOffset = indexWithArrayOffset + index;
                nextIndexKey = index;

                // try current key
                anotherKey = ArrayMemory.getLong(localData, indexWithArrayOffset);

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
                link12 = index | LINK_1_2_SUFIX;
                link34 = index | LINK_3_4_SUFIX;
                link12 = ArrayMemory.getLong(localData, link12);
                link34 = ArrayMemory.getLong(localData, link34);
                link1 = link12 >> INT_SHIFT;
                link2 = link12 & INT_MASK;
                link3 = link34 >> INT_SHIFT;
                link1 = link1 << INDEX_OFFSET_SHIFT;
                link2 = link2 << INDEX_OFFSET_SHIFT;
                link3 = link3 << INDEX_OFFSET_SHIFT;
                link1 = link1 + ARRAY_OFFSET;
                link2 = link2 + ARRAY_OFFSET;
                link3 = link3 + ARRAY_OFFSET;

                // check first link
                if (link1 == 0) {
                    index = ELEMENT_NOT_FOUND;
                    link12Mask = LINK_1_X_MASK;
                    link34Mask = LINK_X_X_MASK;
                    break find;
                }

                // try first link
                anotherKey = ArrayMemory.getLong(localData, link1);
                if (anotherKey == key) {
                    index = link1;
                    break find;
                }

                // check second link
                if (link2 == 0) {
                    index = ELEMENT_NOT_FOUND;
                    link12Mask = LINK_X_2_MASK;
                    link34Mask = LINK_X_X_MASK;
                    break find;
                }

                // try second link
                anotherKey = ArrayMemory.getLong(localData, link2);
                if (anotherKey == key) {
                    index = link2;
                    break find;
                }

                // check third link
                if (link3 == 0) {
                    index = ELEMENT_NOT_FOUND;
                    link12Mask = LINK_X_X_MASK;
                    link34Mask = LINK_3_X_MASK;
                    break find;
                }

                // try third link
                anotherKey = ArrayMemory.getLong(localData, link3);
                if (anotherKey == key) {
                    index = link3;
                    break find;
                }

                // we don't have empty elements in the bucket -> increase table
                enlarge();
                localData = data;
                localCapacity = capacity;
            }

            // find place to insert if we don't know one
            if (index == ELEMENT_NOT_FOUND) {
                index = nextIndexKey;

                allowedDisplace = GUARANTIED_AMOUNT;
                for (; ; ) {
                    index += INDEX_STEP;
                    link34 = index | LINK_3_4_SUFIX;
                    allowedDisplace--;

                    link4 = (int) ArrayMemory.getLong(localData, link34);
                    if (link4 == 0) {
                        // switch key and value
                        temp2 = index | DATA_SUFIX;
                        anotherKey = ArrayMemory.getLong(localData, index);
                        anotherValue = ArrayMemory.getLong(localData, temp2);
                        ArrayMemory.setLong(localData, index, key);
                        ArrayMemory.setLong(localData, temp2, value);

                        // update links
                        temp1 = index >> INDEX_OFFSET_SHIFT;
                        link12 = index | LINK_1_2_SUFIX;
                        temp2 = temp1 << INT_SHIFT;
                        temp1 = temp1 | temp2;
                        temp5 = ArrayMemory.getLong(localData, link34);
                        temp4 = ArrayMemory.getLong(localData, link12);
                        temp2 = temp1 & link12Mask;
                        temp3 = temp1 & link34Mask;
                        temp4 = temp4 | temp2;
                        temp5 = temp5 | temp3;
                        ArrayMemory.setLong(localData, link12, temp4);
                        ArrayMemory.setLong(localData, link34, temp5);

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
            ArrayMemory.setLong(localData, index, key);
            ArrayMemory.setLong(localData, index | DATA_SUFIX, value);

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

        data = newData;
        capacity = newCapacity;

        long key, value;
        for (int i = 0; i < localCapacity; i++) {
            key = localData[i << 1];
            if (key != ELEMENT_NOT_FOUND) {
                int indexKey = i << INDEX_STEP_SHIFT;
                value = ArrayMemory.getLong(localData, indexKey | DATA_SUFIX);


                //set(key, value);

            }
        }
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
