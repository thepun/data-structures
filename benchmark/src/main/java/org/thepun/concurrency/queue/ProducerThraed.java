package org.thepun.concurrency.queue;

import java.util.concurrent.CountDownLatch;

import org.thepun.queue.QueueTail;

final class ProducerThraed extends StartFinishThread {

    private final Long[] values;
    private final int length;
    private final QueueTail<Long> queueTail;

    ProducerThraed(CountDownLatch startLatch, CountDownLatch finishLatch, QueueTail<Long> queueTail, Long[] values) {
        super(startLatch, finishLatch);

        this.values = values;
        this.queueTail = queueTail;

        length = values.length;
    }

    @Override
    void execute() {
        for (int k = 0; k < 100; k++) {
            for (int i = 0; i < length; i++) {
                queueTail.addToTail(values[i]);
            }
        }
    }
}
