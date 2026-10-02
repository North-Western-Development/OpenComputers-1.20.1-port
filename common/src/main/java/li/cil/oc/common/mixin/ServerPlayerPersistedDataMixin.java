package li.cil.oc.common.mixin;

import li.cil.oc.util.PlayerUtils;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps OC's persisted player data across death / dimension change, like
 * Forge does for its {@code PlayerPersisted} tag.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerPersistedDataMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void oc$copyPersistedData(ServerPlayer that, boolean keepEverything, CallbackInfo ci) {
        ((PlayerUtils.PersistedDataHolder) this).oc$setPersistedData(((PlayerUtils.PersistedDataHolder) that).oc$getPersistedData().copy());
    }
}
