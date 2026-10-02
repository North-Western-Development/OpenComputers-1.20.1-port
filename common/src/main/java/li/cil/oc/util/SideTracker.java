package li.cil.oc.util;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Determines the logical side of the current thread, without Forge.
 * <p/>
 * Replaces Forge's <tt>EffectiveSide</tt>: we are on the logical server if
 * running on a dedicated server, if the current thread is the (integrated)
 * server's main thread, or if the current thread was explicitly registered
 * as a server thread via {@link #addServerThread()} (e.g. worker threads
 * spawned by the server).
 */
public final class SideTracker {
    private static final Set<Thread> serverThreads = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private SideTracker() {
    }

    /**
     * Marks the current thread as belonging to the logical server.
     */
    public static void addServerThread() {
        serverThreads.add(Thread.currentThread());
    }

    public static boolean isServer() {
        if (Platform.getEnvironment() == Env.SERVER) {
            return true;
        }
        final Thread current = Thread.currentThread();
        if (serverThreads.contains(current)) {
            return true;
        }
        final MinecraftServer server = GameInstance.getServer();
        if (server != null && server.getRunningThread() == current) {
            return true;
        }
        // Same heuristic Forge used for threads it did not create itself.
        return "Server thread".equals(current.getName());
    }

    public static boolean isClient() {
        return !isServer();
    }
}
