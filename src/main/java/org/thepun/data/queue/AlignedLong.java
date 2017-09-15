package org.thepun.data.queue;

import org.thepun.unsafe.ObjectMemory;
import org.thepun.unsafe.ObjectMemoryLayout;

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
        valueOffset = ObjectMemoryLayout.getFieldOffset(AlignedLongFields.class, "value");
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
        return ObjectMemory.getAndIncrementLong(this, valueOffset, 1L);
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

