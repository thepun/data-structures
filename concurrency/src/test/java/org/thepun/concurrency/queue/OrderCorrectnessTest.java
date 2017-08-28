package org.thepun.concurrency.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.thepun.concurrency.queue.mpsc.RoundRobinMultiplexer;
import org.thepun.concurrency.queue.spsc.LinkedArrayBridge;

@RunWith(Parameterized.class)
public class OrderCorrectnessTest {

    @Parameter(0)
    public QueueTail<Long> tail;

    @Parameter(1)
    public QueueHead<Long> head;

    @Test
    public void emptyQueue() {
        Long element = head.removeFromHead();
        assertNull(element);
    }

    @Test
    public void addAndGet() {
        tail.addToTail(1L);

        Long element = head.removeFromHead();
        assertNotNull(element);
        assertEquals(1L, (long) element);
    }

    @Test
    public void noMoreElements() {
        for (int i = 0; i < 1000; i++) {
            tail.addToTail(1L);
        }

        for (int i = 0; i < 1000; i++) {
            head.removeFromHead();
        }

        Long element = head.removeFromHead();
        assertNull(element);
    }

    @Test
    public void addManyAndGetMany() {
        for (long i = 0; i < 10000000; i++) {
            tail.addToTail(i);
        }

        for (long i = 0; i < 10000000; i++) {
            Long element = head.removeFromHead();
            assertNotNull(element);
            assertEquals(i, (long) element);
        }
    }

    @Test
    public void addBunchAndGetBunchMultipleTimes() {
        for (int l = 0; l < 10000; l++) {
            for (long i = 0; i < 10000; i++) {
                tail.addToTail(i * l);
            }

            for (long i = 0; i < 10000; i++) {
                Long element = head.removeFromHead();
                assertNotNull(element);
                assertEquals(i * l, (long) element);
            }
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> list = new ArrayList<>();

        LinkedArrayBridge<Long> longSPSCSplittedLinkedQueue = new LinkedArrayBridge<>();
        list.add(new Object[] {longSPSCSplittedLinkedQueue, longSPSCSplittedLinkedQueue});

        BlockingLinkedArrayQueue<Long> longSPSCBlockingLinkedQueue = new BlockingLinkedArrayQueue<>();
        list.add(new Object[] {longSPSCBlockingLinkedQueue, longSPSCBlockingLinkedQueue});

        RoundRobinMultiplexer<Long> longLinkedArrayMultiplexer = new RoundRobinMultiplexer<>();
        list.add(new Object[] {longLinkedArrayMultiplexer.createProducer(), longLinkedArrayMultiplexer});

        return list;
    }
}
