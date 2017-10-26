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


import io.github.thepun.unsafe.ArrayMemory;
import io.github.thepun.unsafe.MemoryFence;

public final class RingBufferBridge<T> implements QueueHead<T>, QueueTail<T> {

    // TODO: align local variables
    // TODO: get rid of size field

    private final int size;
    private final int mask;
    private final Object[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;
    private final AlignedLong localReadCounter;
    private final AlignedLong localWriteCounter;

    public RingBufferBridge(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        double log2 = Math.log10(queueSize) / Math.log10(2);
        int pow = (int) Math.ceil(log2);

        size = (int) Math.pow(2, pow);
        mask = size - 1;
        data = new Object[size];
        readCounter = new AlignedLong();
        writeCounter = new AlignedLong();
        localReadCounter = new AlignedLong();
        localWriteCounter = new AlignedLong();
    }

    @Override
    public T removeFromHead() {
        long writeIndex = localWriteCounter.get();
        long readIndex = readCounter.get();
        if (readIndex >= writeIndex) {
            writeIndex = writeCounter.get();
            localWriteCounter.set(writeIndex);

            if (readIndex >= writeIndex) {
                return null;
            }
        }

        int index = (int) (readIndex & mask);
        Object element = ArrayMemory.getObject(data, index);
        MemoryFence.load();
        readCounter.set(readIndex + 1);

        return (T) element;
    }

    @Override
    public boolean addToTail(T element) {
        long readIndex = localReadCounter.get();
        long writeIndex = writeCounter.get();
        if (writeIndex >= readIndex + size) {
            readIndex = readCounter.get();
            localReadCounter.set(readIndex);

            if (readIndex >= writeIndex) {
                return false;
            }
        }

        int index = (int) (writeIndex & mask);
        ArrayMemory.setObject(data, index, element);
        MemoryFence.store();
        writeCounter.set(writeIndex + 1);

        return true;
    }
}
