package unam.ciencias.computoconcurrente;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class SemaphoreTest {

    static int ROUNDS = 100000;
    static int THREADS = 6;
    static int MAX_CONCURRENT_THREADS = 3;

    Semaphore semaphore;
    Thread[] threads;
    boolean isSemaphoreCorrect;
    AtomicInteger atomicInteger;

    @BeforeEach
    public void setup() {
        isSemaphoreCorrect = true;
        atomicInteger = new AtomicInteger(0);
        initThreads();
    }

    void initFilterSemaphore() {
        semaphore = new FilterSemaphoreImpl(THREADS, MAX_CONCURRENT_THREADS);
    }

    void initThreads() {
        threads = new Thread[THREADS];
        for(int i = 0; i < THREADS-1; i++) {
            threads[i] = new Thread(this::acquireRounds, String.format("%d", i));
        }
        threads[THREADS - 1] = new Thread(this::verifySemaphoreIsCorrect);
    }

    void acquireRounds() {
        long myValue = 0;
        for(int i = 0; i < ROUNDS; i++) {
            semaphore.acquire();
            this.atomicInteger.incrementAndGet();
            myValue += this.simulateCriticalSection(Math.random() * 100);
            if((i % 1000) == 0)
                this.sleepCurrentThreads(Math.random() * 10);
            this.atomicInteger.decrementAndGet();
            semaphore.release();
        }
        System.out.printf("%s reached value %d\n", Thread.currentThread().getName(), myValue);
    }

    Integer simulateCriticalSection(Double iterations) {
        int val = 0;
        for(int j = 0; j < iterations; j++) {
            val += j & 1;
        }
        return val;
    }

    void verifySemaphoreIsCorrect() {
        for(int i = 0; i < (THREADS * ROUNDS); i++) {
            for(int j = 0; j < Math.random() * 100; j++) {
                isSemaphoreCorrect = isSemaphoreCorrect && (atomicInteger.get() <= MAX_CONCURRENT_THREADS);
            }
            if((i % 6000) == 0) {
                sleepCurrentThreads(Math.random() * 50);
            }
        }
        System.out.printf("%s finished verification %s\n", Thread.currentThread().getName(), isSemaphoreCorrect);
    }

    void sleepCurrentThreads(Double aproxMilliseconds) {
        try {
            Thread.sleep(aproxMilliseconds.longValue());
        } catch(InterruptedException ie) {
            System.out.printf("%s  - Interrupt exception happened", Thread.currentThread().getName());
            throw new RuntimeException("Unexpected interrupt exception.");
        }

    }

    @Test
    void filterSemaphore() throws InterruptedException {
        initFilterSemaphore();

        for(Thread t :  threads) {
            t.start();
        }
        for(Thread t : threads) {
            t.join();
        }
        assertTrue(isSemaphoreCorrect);
    }
}