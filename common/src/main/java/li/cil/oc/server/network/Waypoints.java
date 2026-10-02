package li.cil.oc.server.network;

import dev.architectury.event.events.common.LifecycleEvent;
import li.cil.oc.Settings;
import li.cil.oc.common.tileentity.Waypoint;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.RTree;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Waypoints {
    public static final Map<ResourceKey<Level>, RTree<Waypoint>> dimensions = new ConcurrentHashMap<>();

    private static boolean initialized;

    private Waypoints() {
    }

    /**
     * Registers the level load/unload handlers (formerly Forge event bus
     * subscriptions). Call once during common init.
     */
    public static synchronized void register() {
        if (initialized) return;
        initialized = true;
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(world -> dimensions.remove(world.dimension()));
        LifecycleEvent.SERVER_LEVEL_LOAD.register(world -> dimensions.remove(world.dimension()));
        // TODO(port): the Forge ChunkEvent.Unload safety clean up has no Architectury
        //  equivalent; waypoints remove themselves in dispose().
    }

    public static void add(Waypoint waypoint) {
        final Level world = waypoint.getLevel();
        if (!waypoint.isRemoved() && world != null && !world.isClientSide) {
            dimensions.computeIfAbsent(world.dimension(), k -> new RTree<Waypoint>(Settings.get().rTreeMaxEntries,
                    w -> Triple.of(w.getBlockPos().getX() + 0.5, w.getBlockPos().getY() + 0.5, w.getBlockPos().getZ() + 0.5))).add(waypoint);
        }
    }

    public static void remove(Waypoint waypoint) {
        final Level world = waypoint.getLevel();
        if (world != null && !world.isClientSide) {
            final RTree<Waypoint> set = dimensions.get(world.dimension());
            if (set != null) set.remove(waypoint);
        }
    }

    public static List<Waypoint> findWaypoints(BlockPosition pos, double range) {
        final RTree<Waypoint> set = dimensions.get(pos.world.get().dimension());
        if (set != null) {
            final AABB bounds = pos.bounds().inflate(range * 0.5, range * 0.5, range * 0.5);
            return set.query(Triple.of(bounds.minX, bounds.minY, bounds.minZ), Triple.of(bounds.maxX, bounds.maxY, bounds.maxZ));
        }
        return Collections.emptyList();
    }
}
