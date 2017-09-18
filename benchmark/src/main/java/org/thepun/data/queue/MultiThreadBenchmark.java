package org.thepun.data.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(jvmArgs = {/*"-verbose:gc",*/ "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class MultiThreadBenchmark {

    //@Param({"16", "8", "4", "2"})
    @Param({"4"})
    private int cpu;

//    @Param({"8", "4", "2", "1"})
    @Param({"2"})
    private int halfCpu;

    private Long[] values;

    @Setup(Level.Iteration)
    public void prepareValues() {
        values = new Long[1_000_000];
        for (int l = 0; l < 1_000_000; l++) {
            values[l] = new Long(l);
        }
    }

    @TearDown(Level.Iteration)
    public void clearValues() throws InterruptedException {
        values = null;
    }

    @Benchmark
    public long ringBufferRouter() throws InterruptedException {
        RingBufferRouter<Long> queue = new RingBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long greedyRingBufferRouter() throws InterruptedException {
        GreedyRingBufferRouter<Long> queue = new GreedyRingBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long atomicPool() throws InterruptedException {
        AtomicPoolRouter<Long> queue = new AtomicPoolRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long atomicBuffer() throws InterruptedException {
        AtomicBufferRouter<Long> queue = new AtomicBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue.createConsumer();
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue.createProducer();
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long arraydBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long concurrentLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ConcurrentLinkedQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcAtomicArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcAtomicArrayQueue<>(10000));

        QueueHead<Long>[] queueHeads = new QueueHead[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueHeads[i] = queue;
        }

        QueueTail<Long>[] queueTails = new QueueTail[halfCpu];
        for (int i = 0; i < halfCpu; i++) {
            queueTails[i] = queue;
        }

        return BenchmarkCases.multipleProducersAndMultipleConsumer(queueHeads, queueTails, values, 100_000_000);
    }

   public static void main(String[] args) throws InterruptedException {
        MultiThreadBenchmark benchmark = new MultiThreadBenchmark();

        while (true) {
            benchmark.prepareValues();
            benchmark.cpu = 4;
            benchmark.halfCpu = 2;
            benchmark.atomicBuffer();
            System.out.println("next");
        }
    }
}
