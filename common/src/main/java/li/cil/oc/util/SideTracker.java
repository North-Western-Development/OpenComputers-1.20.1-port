package li.cil.oc.util;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public final class SideTracker {
    private SideTracker() {
    }

    /**
     * Whether the current thread is a (logical) server thread. Formerly
     * {@code Environment.get().getDist().isDedicatedServer() || EffectiveSide.get().isServer()};
     * now: dedicated server, or any thread that is not the client's render thread
     * (integrated server thread, OC's worker threads, ...).
     */
    public static boolean isServer() {
        return Platform.getEnvironment() == Env.SERVER || !ClientThread.isClientThread();
    }

    public static boolean isClient() {
        return !isServer();
    }

    // Separate class so the dedicated server never resolves client classes.
    private static final class ClientThread {
        static boolean isClientThread() {
            return RenderSystem.isOnRenderThread();
        }
    }
}
