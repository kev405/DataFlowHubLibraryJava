package com;

import org.junit.jupiter.api.Test;

import com.concurrent.future.ReportAggregator;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ReportAggregatorTest {

    @Test
    void generate_executesInParallel_under400ms() throws Exception {
        ReportAggregator agg = new ReportAggregator();

        long t0 = System.currentTimeMillis();
        ReportAggregator.Report r = agg.generate("X").get();   // blocks
        long elapsed = System.currentTimeMillis() - t0;

        assertTrue(elapsed < 400, "should run three tasks in parallel");
        assertNotNull(r);
        assertTrue(r.summary().length() >= 3);
    }

    @Test
    void failingTask_propagatesException() {
        ReportAggregator agg = new ReportAggregator() {
            @Override protected String loadPart(String l) {
                if (l.equals("B")) throw new IllegalStateException("boom");
                return super.loadPart(l);
            }
        };
        CompletableFuture<?> fut = agg.generate("Y");
        ExecutionException ex = assertThrows(ExecutionException.class, fut::get);
        assertEquals("aggregation failed", ex.getCause().getMessage());
    }
}
