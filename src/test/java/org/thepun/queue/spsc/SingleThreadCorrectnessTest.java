package org.thepun.queue.spsc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SingleThreadCorrectnessTest {

    @Test
    public void emptyQueue() {
        SPSCLinkedArrayQueue<Long> queue = new SPSCLinkedArrayQueue<>();

        Long element = queue.removeFromHead();
        assertNull(element);
    }

    @Test
    public void addAndGet() {
        SPSCLinkedArrayQueue<Long> queue = new SPSCLinkedArrayQueue<>();
        queue.addToTail(1L);

        Long element = queue.removeFromHead();
        assertNotNull(element);
        assertEquals(1L, (long) element);
    }

    @Test
    public void noMoreElements() {
        SPSCLinkedArrayQueue<Long> queue = new SPSCLinkedArrayQueue<>();
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
        SPSCLinkedArrayQueue<Long> queue = new SPSCLinkedArrayQueue<>();
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
        SPSCLinkedArrayQueue<Long> queue = new SPSCLinkedArrayQueue<>();
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
}
