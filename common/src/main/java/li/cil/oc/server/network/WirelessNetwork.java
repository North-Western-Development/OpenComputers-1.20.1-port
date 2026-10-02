package li.cil.oc.server.network;

import dev.architectury.event.events.common.LifecycleEvent;
import li.cil.oc.Settings;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedBlock;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.RTree;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WirelessNetwork {
    public static final Map<ResourceKey<Level>, RTree<WirelessEndpoint>> dimensions = new ConcurrentHashMap<>();

    private static boolean initialized;

    private WirelessNetwork() {
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
        // TODO(port): the Forge ChunkEvent.Unload safety clean up (removing endpoints of
        //  unloaded block entities) has no Architectury equivalent; block entities are
        //  expected to leave the network in their dispose().
    }

    public static void add(WirelessEndpoint endpoint) {
        dimensions.computeIfAbsent(dimension(endpoint), k -> new RTree<WirelessEndpoint>(Settings.get().rTreeMaxEntries,
                e -> Triple.of(e.x() + 0.5, e.y() + 0.5, e.z() + 0.5))).add(endpoint);
    }

    public static void update(WirelessEndpoint endpoint) {
        final RTree<WirelessEndpoint> tree = dimensions.get(dimension(endpoint));
        if (tree != null) {
            final Optional<Triple<Double, Double, Double>> position = tree.apply(endpoint);
            if (position.isPresent()) {
                final double dx = Math.abs(endpoint.x() + 0.5 - position.get().getLeft());
                final double dy = Math.abs(endpoint.y() + 0.5 - position.get().getMiddle());
                final double dz = Math.abs(endpoint.z() + 0.5 - position.get().getRight());
                if (dx > 0.5 || dy > 0.5 || dz > 0.5) {
                    tree.remove(endpoint);
                    tree.add(endpoint);
                }
            }
        }
    }

    public static boolean remove(WirelessEndpoint endpoint, ResourceKey<Level> dimension) {
        final RTree<WirelessEndpoint> set = dimensions.get(dimension);
        return set != null && set.remove(endpoint);
    }

    public static boolean remove(WirelessEndpoint endpoint) {
        final RTree<WirelessEndpoint> set = dimensions.get(dimension(endpoint));
        return set != null && set.remove(endpoint);
    }

    public static List<WirelessEndpoint> computeReachableFrom(WirelessEndpoint endpoint, double strength) {
        final RTree<WirelessEndpoint> tree = dimensions.get(dimension(endpoint));
        if (tree != null && strength > 0) {
            final double range = strength + 1;
            final List<WirelessEndpoint> result = new ArrayList<>();
            for (WirelessEndpoint candidate : tree.query(offset(endpoint, -range), offset(endpoint, range))) {
                if (candidate == endpoint) continue;
                final double squaredDistance = squaredDistance(endpoint, candidate);
                if (squaredDistance > range * range) continue;
                if (isUnobstructed(endpoint, strength, Pair.of(candidate, Math.sqrt(squaredDistance)))) {
                    result.add(candidate);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static ResourceKey<Level> dimension(WirelessEndpoint endpoint) {
        return endpoint.world().dimension();
    }

    private static Triple<Double, Double, Double> offset(WirelessEndpoint endpoint, double value) {
        return Triple.of(endpoint.x() + 0.5 + value, endpoint.y() + 0.5 + value, endpoint.z() + 0.5 + value);
    }

    private static double squaredDistance(WirelessEndpoint reference, WirelessEndpoint endpoint) {
        final double dx = endpoint.x() - reference.x();
        final double dy = endpoint.y() - reference.y();
        final double dz = endpoint.z() - reference.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean isUnobstructed(WirelessEndpoint reference, double strength, Pair<WirelessEndpoint, Double> info) {
        final WirelessEndpoint endpoint = info.getLeft();
        final double distance = info.getRight();
        final double gap = distance - 1;
        if (gap > 0) {
            // If there's some space between the two wireless network cards we try to
            // figure out if the signal might have been obstructed. We do this by
            // taking a few samples (more the further they are apart) and check if we
            // hit a block. For each block hit we subtract its hardness from the
            // surplus strength left after crossing the distance between the two. If
            // we reach a point where the surplus strength does not suffice we block
            // the message.
            final Level world = endpoint.world();

            final Vec3 origin = new Vec3(reference.x(), reference.y(), reference.z());
            final Vec3 target = new Vec3(endpoint.x(), endpoint.y(), endpoint.z());

            // Vector from reference endpoint (sender) to this one (receiver).
            final Vec3 delta = subtract(target, origin);
            final Vec3 v = delta.normalize();

            // Get the vectors that are orthogonal to the direction vector.
            final Vec3 up;
            if (v.x == 0 && v.z == 0) {
                assert v.y != 0;
                up = new Vec3(1, 0, 0);
            } else {
                up = new Vec3(0, 1, 0);
            }
            final Vec3 side = crossProduct(v, up);
            final Vec3 top = crossProduct(v, side);

            // Accumulated obstructions and number of samples.
            double hardness = 0.0;
            final int samples = Math.max(1, (int) Math.sqrt(gap));

            for (int i = 0; i < samples; i++) {
                final double rGap = world.random.nextDouble() * gap;
                // Adding some jitter to avoid only tracking the perfect line between
                // two endpoints when they are diagonal to each other for example.
                final int rSide = world.random.nextInt(3) - 1;
                final int rTop = world.random.nextInt(3) - 1;
                final int x = (int) (origin.x + v.x * rGap + side.x * rSide + top.x * rTop);
                final int y = (int) (origin.y + v.y * rGap + side.y * rSide + top.y * rTop);
                final int z = (int) (origin.z + v.z * rGap + side.z * rSide + top.z * rTop);
                final BlockPosition blockPos = BlockPosition.apply(x, y, z, world);
                if (ExtendedWorld.isLoaded(world, blockPos)) {
                    final Block block = ExtendedWorld.getBlock(world, blockPos);
                    if (block != null) {
                        hardness += ExtendedBlock.getBlockHardness(block, blockPos);
                    }
                }
            }

            // Normalize and scale obstructions:
            hardness *= gap / samples;

            // See if we have enough power to overcome the obstructions.
            return strength - gap > hardness;
        } else return true;
    }

    private static Vec3 subtract(Vec3 v1, Vec3 v2) {
        return new Vec3(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }

    private static Vec3 crossProduct(Vec3 v1, Vec3 v2) {
        return new Vec3(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x);
    }
}
