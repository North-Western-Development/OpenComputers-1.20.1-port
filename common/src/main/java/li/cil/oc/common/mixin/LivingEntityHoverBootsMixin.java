package li.cil.oc.common.mixin;

import li.cil.oc.common.event.HoverBootsHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces Forge's LivingJumpEvent / LivingFallEvent for hover boots.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHoverBootsMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void oc$onJump(CallbackInfo ci) {
        HoverBootsHandler.onLivingJump((LivingEntity) (Object) this);
    }

    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float oc$onFall(float distance) {
        return HoverBootsHandler.onLivingFall((LivingEntity) (Object) this, distance);
    }
}
