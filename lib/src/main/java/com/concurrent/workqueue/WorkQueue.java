package com.concurrent.workqueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Simple producer / consumer work-queue backed by a {@link BlockingQueue}.
 * Workers run until they receive a POISON_PILL or are interrupted.
 */
public final class WorkQueue implements AutoCloseable {

    private static final Runnable POISON_PILL = () -> { /* no-op */ };

    private final BlockingQueue<Runnable> queue;
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = false;

    /** Unlimited capacity by default. */
    public WorkQueue() { this(new LinkedBlockingQueue<>()); }

    public WorkQueue(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    /* ------------------------------------------------------------------ */
    /* API                                                                 */
    /* ------------------------------------------------------------------ */

    /** Starts {@code n} worker threads. */
    public synchronized void startWorkers(int n) {
        if (running) throw new IllegalStateException("already running");
        running = true;
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(this::consumeLoop, "work-queue-" + i);
            t.start();
            workers.add(t);
        }
    }

    /** Adds a task to the queue (blocks if queue is full). */
    public void submit(Runnable task) throws InterruptedException {
        Objects.requireNonNull(task);
        queue.put(task);
    }

    /** Graceful stop: sends one POISON_PILL per worker and joins them. */
    public void stop() throws InterruptedException {
        for (int i = 0; i < workers.size(); i++) queue.put(POISON_PILL);
        for (Thread t : workers) t.join();
        running = false;
    }

    @Override public void close() {
        try { stop(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /* ------------------------------------------------------------------ */
    /* Worker loop                                                         */
    /* ------------------------------------------------------------------ */

    private void consumeLoop() {
        try {
            while (true) {
                Runnable task = queue.take();
                if (task == POISON_PILL) break;
                try { task.run(); } catch (Throwable ignored) { /* swallow */ }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // allow thread to exit
        }
    }
}
