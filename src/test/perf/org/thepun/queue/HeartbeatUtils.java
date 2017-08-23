package org.thepun.queue;

import java.util.concurrent.locks.LockSupport;

class HeartbeatUtils {

    private static Thread HEARTBEATER;

    public synchronized static void startHeartbeats(long sensity) {
        HEARTBEATER = new Thread("heartbeater") {
            @Override
            public void run() {
                for (;;) {
                    long start = System.nanoTime();
                    LockSupport.parkNanos(1000);
                    long finish = System.nanoTime();

                    long diff = finish - start;
                    if (diff > 1000 + sensity) {
                        System.out.println("FOUND PAUSE");
                    }
                }
            }
        };

        HEARTBEATER.setPriority(Thread.MAX_PRIORITY);
        HEARTBEATER.setDaemon(true);
        HEARTBEATER.start();
    }
}
