package li.cil.oc.util;

import li.cil.oc.OpenComputers;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Formerly declared in ThreadPoolFactory.scala.
 */
public class SafeThreadPool {
    public final String name;
    public final int threads;
    private ScheduledExecutorService _threadPool;

    public SafeThreadPool(String name, int threads) {
        this.name = name;
        this.threads = threads;
    }

    public Optional<Future<?>> withPool(Function<ScheduledExecutorService, Future<?>> f, boolean requiresPool) {
        if (_threadPool == null) {
            OpenComputers.log.warn("Error handling file saving: Did the server never start?");
            if (requiresPool) {
                OpenComputers.log.warn("Creating new thread pool.");
                newThreadPool();
            } else {
                return Optional.empty();
            }
        } else if (_threadPool.isShutdown() || _threadPool.isTerminated()) {
            OpenComputers.log.warn("Error handling file saving: Thread pool shut down!");
            if (requiresPool) {
                OpenComputers.log.warn("Creating new thread pool.");
                newThreadPool();
            } else {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(f.apply(_threadPool));
    }

    public Optional<Future<?>> withPool(Function<ScheduledExecutorService, Future<?>> f) {
        return withPool(f, true);
    }

    public void newThreadPool() {
        if (_threadPool != null && !_threadPool.isTerminated()) {
            _threadPool.shutdownNow();
        }
        _threadPool = ThreadPoolFactory.create(name, threads);
    }

    public void waitForCompletion() {
        withPool(threadPool -> {
            try {
                threadPool.shutdown();
                boolean terminated = threadPool.awaitTermination(15, TimeUnit.SECONDS);
                if (!terminated) {
                    OpenComputers.log.warn("Warning: Completing all tasks has already taken 15 seconds!");
                    terminated = threadPool.awaitTermination(105, TimeUnit.SECONDS);
                    if (!terminated) {
                        OpenComputers.log.error("Warning: Completing all tasks has already taken two minutes! Aborting");
                        threadPool.shutdownNow();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }, false);
    }
}
