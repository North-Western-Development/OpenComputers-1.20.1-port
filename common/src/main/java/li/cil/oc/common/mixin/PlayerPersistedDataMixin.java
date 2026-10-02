package li.cil.oc.common.mixin;

import li.cil.oc.util.PlayerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loader independent replacement for Forge's player persistent data
 * (see {@link PlayerUtils#persistedData(Player)}).
 */
@Mixin(Player.class)
public abstract class PlayerPersistedDataMixin implements PlayerUtils.PersistedDataHolder {
    @Unique
    private CompoundTag oc$persistedData = new CompoundTag();

    @Override
    public CompoundTag oc$getPersistedData() {
        return oc$persistedData;
    }

    @Override
    public void oc$setPersistedData(CompoundTag data) {
        oc$persistedData = data;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void oc$savePersistedData(CompoundTag nbt, CallbackInfo ci) {
        nbt.put(PlayerUtils.PERSISTED_NBT_TAG, oc$persistedData);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void oc$loadPersistedData(CompoundTag nbt, CallbackInfo ci) {
        oc$persistedData = nbt.getCompound(PlayerUtils.PERSISTED_NBT_TAG);
    }
}
