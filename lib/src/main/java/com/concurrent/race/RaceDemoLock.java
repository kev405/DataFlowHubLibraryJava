package com.concurrent.race;

import java.util.concurrent.locks.ReentrantLock;

public class RaceDemoLock {

    private int counter = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void run(int threads, int iterations) throws InterruptedException {
        Thread[] arr = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            arr[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    lock.lock();
                    try { counter++; }
                    finally { lock.unlock(); }
                }
            });
            arr[i].start();
        }
        for (Thread t : arr) t.join();
    }
    public int counter() { return counter; }
}
