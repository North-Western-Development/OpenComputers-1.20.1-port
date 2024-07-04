package li.cil.oc.util;

import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.forgespi.Environment;

import java.util.Collections;
import java.util.Set;

public final class SideTracker {
    public static boolean isServer() {
        return Environment.get().getDist().isDedicatedServer() || EffectiveSide.get().isServer();
    }

    public static boolean isClient() {
        return !isServer();
    }
}
