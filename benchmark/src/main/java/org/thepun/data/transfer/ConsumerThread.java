package org.thepun.data.transfer;

import java.util.concurrent.CountDownLatch;


final class ConsumerThread extends StartFinishThread {

    private final QueueHead<Long> queueHead;
    private final CaseChecker activeChecker;

    private long result;

    ConsumerThread(CountDownLatch startLatch, CountDownLatch finishLatch, QueueHead<Long> queueHead, CaseChecker activeChecker) {
        super(startLatch, finishLatch);

        this.queueHead = queueHead;
        this.activeChecker = activeChecker;
    }

    long getResult() {
        return result;
    }

    @Override
    void execute() {
        Long value;

        while (activeChecker.isActive()) {
            value = queueHead.removeFromHead();

            if (value != null) {
                result += value.longValue();
                activeChecker.actionCompleted();
            }
        }
    }
}
