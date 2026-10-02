package li.cil.oc.common.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Used by the agent fake player (li.cil.oc.server.agent.PlayerInteractionManagerHelper)
 * to check whether its game mode is currently in the process of breaking a block.
 */
@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {
    @Accessor("isDestroyingBlock")
    boolean oc$isDestroyingBlock();
}
