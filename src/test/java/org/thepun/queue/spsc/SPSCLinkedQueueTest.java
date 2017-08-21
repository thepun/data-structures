package org.thepun.queue.spsc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.thepun.queue.TestUtils;

public class SPSCLinkedQueueTest {

    private volatile long result;

    @Test
    public void addAndGet() throws InterruptedException {
        SPSCLinkedQueue<Long> queue = new SPSCLinkedQueue<>();

        class ProducerThraed extends Thread {
            @Override
            public void run() {
                for (long i = 0; i < 10_000_000; i++) {
                    queue.addToTail(i);
                }
            }
        }

        class ConsumerThread extends Thread {
            @Override
            public void run() {
                long tempValue = 0;
                for (long i = 0; i < 10_000_000; i++) {
                    Long value;
                    do {
                        value = queue.removeFromHead();
                    } while (value == null);

                    assertEquals(i, (long) value);

                    tempValue += value;
                }
                result += tempValue;
            }
        }

        for (int k = 0; k < 100; k++) {
            result = 0;

            ProducerThraed producerThraed = new ProducerThraed();
            producerThraed.start();

            ConsumerThread consumerThread = new ConsumerThread();
            consumerThread.start();

            producerThraed.join();
            consumerThread.join();

            assertEquals(TestUtils.calcTotal(10_000_000, 1), result);

            System.out.println("Iteration done! " + (k + 1) + " of 100");
        }
    }
}
