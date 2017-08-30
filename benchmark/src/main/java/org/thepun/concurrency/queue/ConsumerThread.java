package org.thepun.concurrency.queue;

import java.util.concurrent.CountDownLatch;


final class ConsumerThread extends StartFinishThread {

    private final int count;
    private final QueueHead<Long> queueHead;

    private long result;

    ConsumerThread(CountDownLatch startLatch, CountDownLatch finishLatch, QueueHead<Long> queueHead, int count) {
        super(startLatch, finishLatch);

        this.queueHead = queueHead;
        this.count = count;
    }

    long getResult() {
        return result;
    }

    @Override
    void execute() {
        Long value;

        long tempValue = 0;
        for (int i = 0; i < count; i++) {
            do {
                value = queueHead.removeFromHead();
            } while (value == null);

            tempValue += value.longValue();
        }

        result += tempValue;
    }
}
