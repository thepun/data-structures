package org.thepun.data.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

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
    public void addAndGetAndAddAndGet() {
        Long element;

        tail.addToTail(1L);

        element = head.removeFromHead();
        assertNotNull(element);
        assertEquals(1L, (long) element);

        tail.addToTail(3L);

        element = head.removeFromHead();
        assertNotNull(element);
        assertEquals(3L, (long) element);
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
                boolean result = tail.addToTail(i * l);
                assertTrue(result);
            }

            for (long i = 0; i < 10000; i++) {
                if (i == 0 && l == 1999) {
                    Object o = null;
                }

                Long element = head.removeFromHead();
                assertNotNull(element);
                assertEquals(i * l, (long) element);
            }
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> list = new ArrayList<>();

        /*LinkedBridge<Long> longSPSCSplittedLinkedQueue = new LinkedBridge<>();
        list.add(new Object[] {longSPSCSplittedLinkedQueue, longSPSCSplittedLinkedQueue});

        RoundRobinLinkedMultiplexer<Long> longLinkedArrayMultiplexer = new RoundRobinLinkedMultiplexer<>();
        list.add(new Object[] {longLinkedArrayMultiplexer.createProducer(), longLinkedArrayMultiplexer});

        RoundRobinLinkedDemultiplexer<Long> longLinkedArrayDemultiplexer = new RoundRobinLinkedDemultiplexer<>();
        list.add(new Object[] {longLinkedArrayDemultiplexer, longLinkedArrayDemultiplexer.createConsumer()});

        RingBufferBridge<Long> arrayBridge = new RingBufferBridge<>(10000000);
        list.add(new Object[] {arrayBridge, arrayBridge});

        RingBufferRouter<Long> arrayQueue = new RingBufferRouter<>(10000000);
        list.add(new Object[] {arrayQueue.createProducer(), arrayQueue.createConsumer()});*/

        RingBufferRouterWithXADD<Long> arrayQueueWithXADD = new RingBufferRouterWithXADD<>(10000000);
        list.add(new Object[] {arrayQueueWithXADD.createProducer(), arrayQueueWithXADD.createConsumer()});

        return list;
    }
}
