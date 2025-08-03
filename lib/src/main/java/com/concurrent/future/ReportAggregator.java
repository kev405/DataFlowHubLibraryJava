package com.concurrent.future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.utils.error.ErrorHandler;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Aggregates three independent & I/O-bound tasks in parallel and
 * produces a {@link Report}. Uses CompletableFuture composition.
 */
public class ReportAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(ReportAggregator.class);

    /* --------------------------------------------------------------
       Public API
       -------------------------------------------------------------- */

    /**
     * Launches three async tasks (A, B, C) and returns a future
     * that completes with a {@link Report}.
     */
    public CompletableFuture<Report> generate(String reportId) {

        CompletableFuture<String> partA =
                CompletableFuture.supplyAsync(() -> loadPart("A"));

        CompletableFuture<String> partB =
                CompletableFuture.supplyAsync(() -> loadPart("B"));

        CompletableFuture<String> partC =
                CompletableFuture.supplyAsync(() -> loadPart("C"));

        /* A + B  → AB,   AB + C → ABC */
        return partA.thenCombine(partB, (a, b) -> a + b)
                    .thenCombine(partC, (ab, c) -> ab + c)
                    .thenApply(this::kpiFromRaw)
                    .exceptionally(ex -> {
                        ErrorHandler.log(LOG, ex, false);
                        throw new RuntimeException("aggregation failed", ex);
                    });
    }

    /* --------------------------------------------------------------
       Private and protected helpers (simulate slow I/O with Thread.sleep)
       -------------------------------------------------------------- */

    protected String loadPart(String label) {
        try { Thread.sleep(200); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return label;
    }

    private Report kpiFromRaw(String raw) {
        return new Report("R-" + raw.hashCode(), raw, Instant.now());
    }

    /* Simple DTO for demo */
    public record Report(String id, String summary, Instant createdAt) { }
}
