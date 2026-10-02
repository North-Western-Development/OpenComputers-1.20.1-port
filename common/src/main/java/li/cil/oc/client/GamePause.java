package li.cil.oc.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only helper used by the server-side machine executor to detect when
 * the integrated server's game is paused. Only call this when running on the
 * physical client.
 */
public final class GamePause {
    private GamePause() {
    }

    public static boolean isPaused() {
        return Minecraft.getInstance().isPaused();
    }
}
