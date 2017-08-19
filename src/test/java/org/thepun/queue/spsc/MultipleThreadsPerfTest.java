package org.thepun.queue.spsc;

import java.util.concurrent.CountDownLatch;

public class MultipleThreadsPerfTest {

    private static final int N = 100_000_000;

    private static volatile long startTime;
    private static volatile long finishTime;
    private static volatile long result;

    public static void main(String[] args) throws InterruptedException {
        final Long[] values = new Long[N];
        for (int l = 0; l < N; l++) {
            values[l] = new Long(l);
        }

        System.out.println("Initialized!");

        final SPSCLinkedArrayQueue<Long> queue = new SPSCLinkedArrayQueue<>();

        for (int k = 0; k < 1000; k++) {
            result = 0;
            startTime = 0;
            finishTime = 0;

            final CountDownLatch START = new CountDownLatch(1);
            final CountDownLatch FINISH = new CountDownLatch(2);

            class ProducerThraed extends Thread {
                @Override
                public void run() {
                    try {
                        START.await();
                    } catch (InterruptedException e) {
                        return;
                    }

                    startTime = System.nanoTime();

                    for (int i = 0; i < N; i++) {
                        queue.addToTail(values[i]);
                    }

                    FINISH.countDown();
                }
            }

            class ConsumerThread extends Thread {
                @Override
                public void run() {
                    try {
                        START.await();
                    } catch (InterruptedException e) {
                        return;
                    }

                    Long value;

                    long tempValue = 0;
                    for (int i = 0; i < N; i++) {
                        do {
                            value = queue.removeFromHead();
                        } while (value == null);

                        tempValue += value.longValue();
                    }

                    finishTime = System.nanoTime();

                    result += tempValue;

                    FINISH.countDown();
                }
            }

            ProducerThraed producerThraed = new ProducerThraed();
            ConsumerThread consumerThread = new ConsumerThread();
            producerThraed.start();
            consumerThread.start();

            System.out.println("Started!");

            START.countDown();
            FINISH.await();

            System.out.println("Time: " + ((finishTime - startTime) / 1_000_000) + "ms");
            System.out.println("Final result: " + result);

            //System.gc();
            Thread.sleep(1000);
        }
    }


}
