package org.thepun.data.queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
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
public class FourThreadMultiplexerBenchmark {

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

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long roundRobin() throws InterruptedException {
        UnfairLinkedChunkMultiplexer<Long> queue = new UnfairLinkedChunkMultiplexer<>();

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long arrayBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<Long>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long concurrentLinkedQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ConcurrentLinkedQueue<>());

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<Long>());

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpscArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpscArrayQueue<>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcArrayQueue<>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[2];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long mpmcAtomicArrayQueue() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new MpmcAtomicArrayQueue<>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[2];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

   /*public static void main(String[] args) throws InterruptedException {
        FourThreadBenchmark benchmark = new FourThreadBenchmark();

        while (true) {
            benchmark.prepareValues();
            benchmark.ringBufferRouterWithXADDMultiplexer();
            System.out.println("next");
        }
    }*/
}
