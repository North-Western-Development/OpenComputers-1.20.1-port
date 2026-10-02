package li.cil.oc.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
public abstract class ComponentTracker {
    private final Map<ResourceKey<Level>, Cache<String, ManagedEnvironment>> worlds = new HashMap<>();

    private Cache<String, ManagedEnvironment> components(Level world) {
        return worlds.computeIfAbsent(world.dimension(), k -> CacheBuilder.newBuilder().weakValues().<String, ManagedEnvironment>build());
    }

    public synchronized void add(Level world, String address, ManagedEnvironment component) {
        components(world).put(address, component);
    }

    public synchronized void remove(Level world, ManagedEnvironment component) {
        final Cache<String, ManagedEnvironment> cache = components(world);
        final List<String> keys = new ArrayList<>();
        for (Map.Entry<String, ManagedEnvironment> entry : cache.asMap().entrySet()) {
            if (entry.getValue() == component) keys.add(entry.getKey());
        }
        cache.invalidateAll(keys);
        cache.cleanUp();
    }

    public synchronized Optional<ManagedEnvironment> get(Level world, String address) {
        final Cache<String, ManagedEnvironment> cache = components(world);
        cache.cleanUp();
        return Optional.ofNullable(cache.getIfPresent(address));
    }

    /**
     * Formerly a Forge {@code WorldEvent.Unload} subscriber. Must be called by
     * whoever registers the concrete tracker (server: {@code LifecycleEvent.SERVER_LEVEL_UNLOAD},
     * registered in {@link Proxy#preInit()}; client: the client proxy).
     */
    public void onWorldUnload(LevelAccessor world) {
        if (world instanceof Level level) clear(level);
    }

    protected synchronized void clear(Level world) {
        final Cache<String, ManagedEnvironment> cache = components(world);
        cache.invalidateAll();
        cache.cleanUp();
    }
}
