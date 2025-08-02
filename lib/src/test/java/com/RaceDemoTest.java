package com;

import org.junit.jupiter.api.Test;

import com.concurrent.race.RaceDemoAtomic;
import com.concurrent.race.RaceDemoBuggy;
import com.concurrent.race.RaceDemoLock;

import static org.junit.jupiter.api.Assertions.*;

class RaceDemoTest {

    private static final int THREADS = 2_000;
    private static final int ITER = 1_000;          // 1k × 2k = 2 000 000

    @Test
    void buggyVersion_losesUpdates() throws Exception {
        RaceDemoBuggy demo = new RaceDemoBuggy();
        demo.run(THREADS, ITER);
        assertTrue(demo.counter() < THREADS * ITER,
                   "buggy count should miss increments");
    }

    @Test
    void atomicVersion_countsExactly() throws Exception {
        RaceDemoAtomic demo = new RaceDemoAtomic();
        long t0 = System.currentTimeMillis();
        demo.run(THREADS, ITER);
        long elapsed = System.currentTimeMillis() - t0;

        assertEquals(THREADS * ITER, demo.counter());
        assertTrue(elapsed < 2_000);                 // < 2 s
    }

    @Test
    void lockVersion_countsExactly() throws Exception {
        RaceDemoLock demo = new RaceDemoLock();
        demo.run(THREADS, ITER);
        assertEquals(THREADS * ITER, demo.counter());
    }
}
