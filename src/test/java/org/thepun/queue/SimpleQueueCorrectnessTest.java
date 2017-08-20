package org.thepun.queue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.thepun.queue.spsc.SPSCBlockingLinkedQueue;
import org.thepun.queue.spsc.SPSCLinkedQueue;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class SimpleQueueCorrectnessTest {

    @Parameter
    public SimpleQueue<Long> queue;

    @Test
    public void emptyQueue() {
        Long element = queue.removeFromHead();
        assertNull(element);
    }

    @Test
    public void addAndGet() {
        queue.addToTail(1L);

        Long element = queue.removeFromHead();
        assertNotNull(element);
        assertEquals(1L, (long) element);
    }

    @Test
    public void noMoreElements() {
        for (int i = 0; i < 1000; i++) {
            queue.addToTail(1L);
        }

        for (int i = 0; i < 1000; i++) {
            queue.removeFromHead();
        }

        Long element = queue.removeFromHead();
        assertNull(element);
    }

    @Test
    public void addManyAndGetMany() {
        for (long i = 0; i < 10000000; i++) {
            queue.addToTail(i);
        }

        for (long i = 0; i < 10000000; i++) {
            Long element = queue.removeFromHead();
            assertNotNull(element);
            assertEquals(i, (long) element);
        }
    }

    @Test
    public void addBunchAndGetBunchMultipleTimes() {
        for (int l = 0; l < 10000; l++) {
            for (long i = 0; i < 10000; i++) {
                queue.addToTail(i * l);
            }

            for (long i = 0; i < 10000; i++) {
                Long element = queue.removeFromHead();
                assertNotNull(element);
                assertEquals(i * l, (long) element);
            }
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{new SPSCLinkedQueue<Long>()},
                new Object[]{new SPSCBlockingLinkedQueue<Long>()}
                /*new Object[]{new MPSCQueue<Long>()}*/);
    }
}
