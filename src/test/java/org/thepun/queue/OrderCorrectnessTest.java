package org.thepun.queue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.thepun.queue.mpsc.MPSCLinkedMultiplexer;
import org.thepun.queue.spsc.SPSCBlockingLinkedQueue;
import org.thepun.queue.spsc.SPSCLinkedQueue;
import org.thepun.queue.spsc.SPSCSplittedLinkedQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

        SPSCLinkedQueue<Long> longSPSCLinkedQueue = new SPSCLinkedQueue<>();
        list.add(new Object[] {longSPSCLinkedQueue, longSPSCLinkedQueue});

        SPSCSplittedLinkedQueue<Long> longSPSCSplittedLinkedQueue = new SPSCSplittedLinkedQueue<>();
        list.add(new Object[] {longSPSCSplittedLinkedQueue, longSPSCSplittedLinkedQueue});

        SPSCBlockingLinkedQueue<Long> longSPSCBlockingLinkedQueue = new SPSCBlockingLinkedQueue<>();
        list.add(new Object[] {longSPSCBlockingLinkedQueue, longSPSCBlockingLinkedQueue});

        MPSCLinkedMultiplexer<Long> longMPSCLinkedMultiplexer = new MPSCLinkedMultiplexer<>();
        list.add(new Object[] {longMPSCLinkedMultiplexer.createProducer(), longMPSCLinkedMultiplexer});

        return list;
    }
}
