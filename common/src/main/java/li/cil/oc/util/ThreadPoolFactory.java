package li.cil.oc.util;

import dev.architectury.event.events.common.LifecycleEvent;
import li.cil.oc.Settings;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.server.fs.Buffered;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPoolFactory {
    private ThreadPoolFactory() {
    }

    private static Integer priority;

    public static int priority() {
        if (priority == null) {
            final int custom = Settings.get().threadPriority;
            if (custom < 1) priority = Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2;
            else priority = Math.min(Math.max(custom, Thread.MIN_PRIORITY), Thread.MAX_PRIORITY);
        }
        return priority;
    }

    /**
     * Registers the server lifecycle listeners (formerly Forge event subscriptions
     * on {@code FMLServerAboutToStartEvent} / {@code FMLServerStoppedEvent}).
     * Called from {@link li.cil.oc.OpenComputers#init()}.
     */
    public static void init() {
        LifecycleEvent.SERVER_BEFORE_START.register(server -> serverStart());
        LifecycleEvent.SERVER_STOPPED.register(server -> serverStop());
    }

    public static void serverStart() {
        // Access these handles to ensure the pools actually exist.
        final SafeThreadPool stateSaveHandler = SaveHandler.stateSaveHandler;
        final SafeThreadPool fileSaveHandler = Buffered.fileSaveHandler;
        synchronized (safePools) {
            for (SafeThreadPool pool : safePools) {
                pool.newThreadPool();
            }
        }
    }

    public static void serverStop() {
        synchronized (safePools) {
            for (SafeThreadPool pool : safePools) {
                pool.waitForCompletion();
            }
        }
    }

    public static ScheduledExecutorService create(String name, int threads) {
        return Executors.newScheduledThreadPool(threads,
                new ThreadFactory() {
                    private final String baseName = "OpenComputers-" + name + "-";

                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @SuppressWarnings("removal")
                    private final ThreadGroup group = System.getSecurityManager() == null
                            ? Thread.currentThread().getThreadGroup()
                            : System.getSecurityManager().getThreadGroup();

                    @Override
                    public Thread newThread(Runnable r) {
                        final Thread thread = new Thread(group, r, baseName + threadNumber.getAndIncrement());
                        if (!thread.isDaemon()) {
                            thread.setDaemon(true);
                        }
                        if (thread.getPriority() != priority()) {
                            thread.setPriority(priority());
                        }
                        return thread;
                    }
                });
    }

    public static final List<SafeThreadPool> safePools = new ArrayList<>();

    public static SafeThreadPool createSafePool(String name, int threads) {
        final SafeThreadPool handler = new SafeThreadPool(name, threads);
        synchronized (safePools) {
            safePools.add(handler);
        }
        return handler;
    }
}
