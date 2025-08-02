package com.concurrent.race;

import java.util.concurrent.atomic.AtomicInteger;

public class RaceDemoAtomic {

    private final AtomicInteger counter = new AtomicInteger();

    public void run(int threads, int iterations) throws InterruptedException {
        Thread[] arr = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            arr[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) counter.incrementAndGet();
            });
            arr[i].start();
        }
        for (Thread t : arr) t.join();
    }
    public int counter() { return counter.get(); }
}