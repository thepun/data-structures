package org.thepun.data.transfer;

import java.util.concurrent.CountDownLatch;


final class ProducerThraed extends StartFinishThread {

    private final int length;
    private final Long[] values;
    private final QueueTail<Long> queueTail;

    ProducerThraed(CountDownLatch startLatch, CountDownLatch finishLatch, QueueTail<Long> queueTail, Long[] values, int count) {
        super(startLatch, finishLatch);

        this.values = values;
        this.queueTail = queueTail;

        length = count;
    }

    @Override
    void execute() {
        int arraySize = values.length;

        int c = 0;
        for (int i = 0; i < length; i++) {
            for (; ; ) {
                if (queueTail.addToTail(values[c])) {
                    break;
                }
            }

            c++;
            if (c == arraySize) {
                c = 0;
            }
        }
    }
}
