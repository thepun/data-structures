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
package io.github.thepun.data.transfer;


import io.github.thepun.unsafe.ObjectMemory;

/**
 * Internal class for storing long value aligned to cache lines for lesser false sharing.
 *
 * Variable that stores the value is not volatile. Use volatileGet / volatileSet methods to cross memory barriers.
 *
 * Provides Compare-and-Swap (CAS) operation.
 */
final class AlignedLong extends AlignedLongFields {

    static final long valueOffset;
    static {
        valueOffset = ObjectMemory.fieldOffset(AlignedLongFields.class, "value");
    }

    // 56 bytes gap
    private long t1, t2, t3, t4, t5, t6, t7, t8;


    long get() {
        return value;
    }

    void set(long newValue) {
        value = newValue;
    }

    boolean compareAndSwap(long expectedValue, long newValue) {
        return ObjectMemory.compareAndSwapLong(this, valueOffset, expectedValue, newValue);
    }

    long getAndIncrement() {
        return ObjectMemory.getAndAddLong(this, valueOffset, 1L);
    }
}

class AlignedLongPadding {
    // 12 bytes header

    // 4 byte gap
    private int t0;

    // 48 byte gap
    private long t1, t2, t3, t4, t5, t6;
}

class AlignedLongFields extends AlignedLongPadding {

    // non-volatile field to store current value
    long value;

}

