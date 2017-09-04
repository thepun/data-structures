package org.thepun.data.queue;

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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;


@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(jvmArgs = {/*"-verbose:gc",*/ "-XX:+PrintGCDetails", "-server", "-XX:+UseSerialGC", "-Xmn8000M", "-Xms10000M", "-Xmx10000M"})
public class FourThreadBenchmark {

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
    public long ringBufferRouterMultiplexer() throws InterruptedException {
        RingBufferRouter<Long> queue = new RingBufferRouter<>(10000);

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long ringBufferRouterWithXADDMultiplexer() throws InterruptedException {
        RingBufferRouterWithXADD<Long> queue = new RingBufferRouterWithXADD<>(10000);

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue.createConsumer(), queueTails, values, 100_000_000);
    }

    @Benchmark
    public long ringBufferRouterDemultiplexer() throws InterruptedException {
        RingBufferRouter<Long> queue = new RingBufferRouter<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue.createConsumer();
        queueHeads[1] = queue.createConsumer();
        queueHeads[2] = queue.createConsumer();

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue.createProducer(), values, 100_000_000);
    }

    @Benchmark
    public long ringBufferRouterWithXADDDemultiplexer() throws InterruptedException {
        RingBufferRouterWithXADD<Long> queue = new RingBufferRouterWithXADD<>(10000);

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue.createConsumer();
        queueHeads[1] = queue.createConsumer();
        queueHeads[2] = queue.createConsumer();

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue.createProducer(), values, 100_000_000);
    }

    /*@Benchmark
    public long ringBufferRouterSimetric() throws InterruptedException {
        RingBufferRouter<Long> queue = new RingBufferRouter<>(1000);
        return BenchmarkCases.singleProducerAndSingleConsumer(queue.createConsumer(), queue.createProducer(), values);
    }*/

    @Benchmark
    public long roundRobinMultiplexer() throws InterruptedException {
        RoundRobinLinkedMultiplexer<Long> queue = new RoundRobinLinkedMultiplexer<>();

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue.createProducer();
        queueTails[1] = queue.createProducer();
        queueTails[2] = queue.createProducer();

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long roundRobinDemultiplexer() throws InterruptedException {
        RoundRobinLinkedDemultiplexer<Long> queue = new RoundRobinLinkedDemultiplexer<>();

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue.createConsumer();
        queueHeads[1] = queue.createConsumer();
        queueHeads[2] = queue.createConsumer();

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long arrayBlockingQueueMultiplexer() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long arrayBlockingQueueDemultiplexer() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new ArrayBlockingQueue<Long>(1000));

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue;
        queueHeads[1] = queue;
        queueHeads[2] = queue;

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueueMultiplexer() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<Long>(1000));

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedBlockingQueueDemultiplexer() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedBlockingQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue;
        queueHeads[1] = queue;
        queueHeads[2] = queue;

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueueMultiplexer() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<Long>());

        QueueTail<Long>[] queueTails = new QueueTail[3];
        queueTails[0] = queue;
        queueTails[1] = queue;
        queueTails[2] = queue;

        return BenchmarkCases.multipleProducersAndSingleConsumer(queue, queueTails, values, 100_000_000);
    }

    @Benchmark
    public long linkedTransferQueueDemultiplexer() throws InterruptedException {
        QueueAdapter<Long> queue = new QueueAdapter<>(new LinkedTransferQueue<>());

        QueueHead<Long>[] queueHeads = new QueueHead[3];
        queueHeads[0] = queue;
        queueHeads[1] = queue;
        queueHeads[2] = queue;

        return BenchmarkCases.singleProducerAndMultipleConsumers(queueHeads, queue, values, 100_000_000);
    }

   /* public static void main(String[] args) throws InterruptedException {
        FourThreadBenchmark benchmark = new FourThreadBenchmark();

        while (true) {
            benchmark.prepareValues();
            benchmark.ringBufferRouterWithXADDDemultiplexer();
            System.out.println("next");
        }
    }*/
}
