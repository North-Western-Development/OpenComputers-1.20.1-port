package li.cil.oc.common.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the protected chunk holder list of a {@link ChunkMap}; replaces the
 * reflective {@code ObfuscationReflectionHelper} access in {@code common.EventHandler}.
 */
@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    @Invoker("getChunks")
    Iterable<ChunkHolder> oc$getChunks();
}
