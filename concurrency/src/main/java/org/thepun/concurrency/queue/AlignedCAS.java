package org.thepun.concurrency.queue;

import org.thepun.unsafe.Atomic;
import org.thepun.unsafe.Fence;
import org.thepun.unsafe.ObjectMemoryLayout;

final class AlignedCAS {

    private static final long valueOffset;
    static {
        valueOffset = ObjectMemoryLayout.getFieldMemoryOffset(AlignedCAS.class, "value");
    }


    // 12 bytes header
    // 52 bytes gap before
    private int before1, before2,
            before3, before4, before5,
            before6, before7, before8,
            before9, before10, before11,
            before12, before13;

    private long value;

    // 56 bytes gap
    private long after1, after2,
            after3, after4, after5,
            after6, after7;


    public long get() {
        return value;
    }

    public void set(long newValue) {
        value = newValue;
        Fence.store();
    }

    /*public long increment() {
        long current;
        do {
            current = value;
        } while (!Atomic.compareAndSwapLong(this, valueOffset, current, current + 1L));

        return current + 1L;
    }

    public long increment(long previousValue) {
        long current = previousValue;
        while (!Atomic.compareAndSwapLong(this, valueOffset, current, current + 1L)) {
            current = value;
        }

        return current + 1L;
    }*/

    public long tryGetAndIncrement(long upperLimit) {
        long current;
        do {
            current = value;
            if (current >= upperLimit) {
                return -1;
            }
        } while (!Atomic.compareAndSwapLong(this, valueOffset, current, current + 1L));

        return current;
    }

    public boolean compareAndSwap(long expectedValue, long newValue) {
        return Atomic.compareAndSwapLong(this, valueOffset, expectedValue, newValue);
    }
}
