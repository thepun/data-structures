package org.thepun.data.transfer;

import java.util.concurrent.CountDownLatch;

abstract class StartFinishThread extends Thread {

    private final CountDownLatch startLatch;
    private final CountDownLatch finishLatch;

    StartFinishThread(CountDownLatch startLatch, CountDownLatch finishLatch) {
        this.startLatch = startLatch;
        this.finishLatch = finishLatch;
    }

    @Override
    public final void run() {
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            return;
        }

        try {
            execute();
        } finally {
            finishLatch.countDown();
        }
    }

    abstract void execute();
}
