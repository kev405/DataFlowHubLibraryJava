package com.concurrent.race;

/**
 * Demonstrates a data race: 2 000 threads increment the same non-volatile int.
 */
public class RaceDemoBuggy {

    private int counter = 0;                      // NOT thread-safe

    public void run(int threads, int iterations) throws InterruptedException {
        Thread[] arr = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            arr[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) counter++;
            });
            arr[i].start();
        }
        for (Thread t : arr) t.join();
    }
    public int counter() { return counter; }
    
}
