package li.cil.oc.common.mixin;

import li.cil.oc.server.agent.PlayerInteractionManagerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets agents (robots) with an experience upgrade absorb the experience of
 * blocks they break, instead of it being dropped as orbs. Replaces listening
 * to Forge's {@code BlockEvent.BreakEvent#setExpToDrop}.
 */
@Mixin(Block.class)
public abstract class BlockPopExperienceMixin {
    @Inject(method = "popExperience", at = @At("HEAD"), cancellable = true)
    private void oc$capturePopExperience(ServerLevel level, BlockPos pos, int amount, CallbackInfo ci) {
        if (PlayerInteractionManagerHelper.tryCaptureExperience(amount)) {
            ci.cancel();
        }
    }
}
