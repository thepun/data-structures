package org.thepun.concurrency.queue;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by thepun on 20.08.17.
 */
public class BlockingCorrectnessTest {

    //@Test
    public void addAndGet() throws TimeoutException, InterruptedException {
        BlockingLinkedArrayQueue<Long> queue = new BlockingLinkedArrayQueue<>();

        class ProducerThraed extends Thread {
            @Override
            public void run() {
                for (long i = 0; i < 1000; i++) {
                    queue.addToTail(i);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        for (int k = 0; k < 100; k++) {
            ProducerThraed thraed = new ProducerThraed();
            thraed.start();

            long tempValue = 0;
            for (long i = 0; i < 1000; i++) {
                Long value = queue.removeFromBlockingHead(1, TimeUnit.SECONDS);
                assertEquals(i, (long) value);
                tempValue += value;
            }

            assertEquals(TestUtils.calcTotal(1000, 1), tempValue);

            System.out.println("Iteration done! " + (k + 1) + " of 100");
        }
    }

}
