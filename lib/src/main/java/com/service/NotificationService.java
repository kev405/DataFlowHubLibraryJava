package com.service;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.entity.Notification;

/**
 * Sends notifications asynchronously using a fixed thread-pool.
 * The service is reusable and must be shut down gracefully.
 */
public final class NotificationService implements AutoCloseable {

    private final ExecutorService pool;

    /** Creates a service with {@code poolSize} fixed threads. */
    public NotificationService(int poolSize) {
        if (poolSize <= 0) throw new IllegalArgumentException("poolSize <= 0");
        this.pool = Executors.newFixedThreadPool(poolSize);
    }

    /** Default constructor: uses 4 threads as a sensible default. */
    public NotificationService() {
        this(4);
    }

    /**
     * Enqueues the send-task and returns a {@link Future} representing success.
     * Internally we just sleep 200 ms to mimic an e-mail / log write.
     */
    public Future<Boolean> send(Notification n) {
        Objects.requireNonNull(n, "notification");
        return pool.submit(() -> {
            try {
                // simulate I/O latency
                Thread.sleep(200);
                // here you would call JavaMail / Slack API etc.
                System.out.printf("Sent to %s : %s%n", n.recipient(), n.message());
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
    }

    /* ---------------------------------------------------------
       Graceful shutdown
       --------------------------------------------------------- */

    public void shutdown() {
        pool.shutdown();                     // stop accepting new tasks
    }

    /**
     * Blocks until termination or timeout.
     * @return true if terminated within timeout
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
                                    throws InterruptedException {
        return pool.awaitTermination(timeout, unit);
    }

    /** AutoCloseable bridge. */
    @Override public void close() {
        shutdown();
        try { awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
