package org.thepun.data.queue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
public class CASBenchmark {

    private static final int N = 10 * 1024 * 1024;


    private AlignedLong value1;
    private AlignedLong value2;
    private AlignedLong value3;
    private AlignedLong value4;
    private AtomicLong result;

    @Setup(Level.Iteration)
    public void prepareData() {
        value1 = new AlignedLong();
        value1.set(1L);

        value2 = new AlignedLong();
        value2.set(1L);

        value3 = new AlignedLong();
        value3.set(1L);

        value4 = new AlignedLong();
        value4.set(1L);

        result = new AtomicLong();
    }

    @Benchmark
    public long oneCas_8() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(8);

        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + result.get();
    }

    @Benchmark
    public long twoCas_8() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(8);

        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + result.get();
    }

    @Benchmark
    public long fourCas_8() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(8);

        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value3, startLatch, finishLatch, result).start();
        new CasThread(value4, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value3, startLatch, finishLatch, result).start();
        new CasThread(value4, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + value3.get() + value4.get() + result.get();
    }

    @Benchmark
    public long oneFad_8() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(8);

        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + result.get();
    }

    @Benchmark
    public long twoFad_8() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(8);

        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + result.get();
    }

    @Benchmark
    public long fourFad_8() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(8);

        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value3, startLatch, finishLatch, result).start();
        new FadThread(value4, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value3, startLatch, finishLatch, result).start();
        new FadThread(value4, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + value3.get() + value4.get() + result.get();
    }

    @Benchmark
    public long oneCas_16() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(16);

        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + result.get();
    }

    @Benchmark
    public long twoCas_16() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(16);

        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + result.get();
    }

    @Benchmark
    public long fourCas_16() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(16);

        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value3, startLatch, finishLatch, result).start();
        new CasThread(value4, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value3, startLatch, finishLatch, result).start();
        new CasThread(value4, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value3, startLatch, finishLatch, result).start();
        new CasThread(value4, startLatch, finishLatch, result).start();
        new CasThread(value1, startLatch, finishLatch, result).start();
        new CasThread(value2, startLatch, finishLatch, result).start();
        new CasThread(value3, startLatch, finishLatch, result).start();
        new CasThread(value4, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + value3.get() + value4.get() + result.get();
    }

    @Benchmark
    public long oneFad_16() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(16);

        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + result.get();
    }

    @Benchmark
    public long twoFad_16() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(16);

        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + result.get();
    }

    @Benchmark
    public long fourFad_16() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(16);

        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value3, startLatch, finishLatch, result).start();
        new FadThread(value4, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value3, startLatch, finishLatch, result).start();
        new FadThread(value4, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value3, startLatch, finishLatch, result).start();
        new FadThread(value4, startLatch, finishLatch, result).start();
        new FadThread(value1, startLatch, finishLatch, result).start();
        new FadThread(value2, startLatch, finishLatch, result).start();
        new FadThread(value3, startLatch, finishLatch, result).start();
        new FadThread(value4, startLatch, finishLatch, result).start();

        startLatch.countDown();
        finishLatch.await();

        return value1.get() + value2.get() + value3.get() + value4.get() + result.get();
    }


    private static final class FadThread extends Thread {

        private final AlignedLong v;
        private final AlignedLong fadValue;
        private final CountDownLatch startLatch;
        private final CountDownLatch finishLatch;
        private final AtomicLong result;

        private FadThread(AlignedLong fadValue, CountDownLatch startLatch, CountDownLatch finishLatch, AtomicLong result) {
            this.fadValue = fadValue;
            this.startLatch = startLatch;
            this.finishLatch = finishLatch;
            this.result = result;

            v = new AlignedLong();
            v.set(1L);
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }

            for (int i = 0; i < N; i++) {
                long currentValue = fadValue.getAndIncrement();
                v.set(currentValue);
            }

            result.set(v.get());
            finishLatch.countDown();
        }
    }


    private static final class CasThread extends Thread {

        private final AlignedLong v;
        private final AlignedLong casValue;
        private final CountDownLatch startLatch;
        private final CountDownLatch finishLatch;
        private final AtomicLong result;

        private CasThread(AlignedLong casValue, CountDownLatch startLatch, CountDownLatch finishLatch, AtomicLong result) {
            this.casValue = casValue;
            this.startLatch = startLatch;
            this.finishLatch = finishLatch;
            this.result = result;

            v = new AlignedLong();
            v.set(1L);
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }

            for (int i = 0; i < N; i++) {
                long currentValue = v.get();

                while (!casValue.compareAndSwap(currentValue, currentValue + 1)) {
                    currentValue = casValue.get();
                }

                v.set(currentValue + 1);
            }

            result.set(v.get());
            finishLatch.countDown();
        }
    }
}
