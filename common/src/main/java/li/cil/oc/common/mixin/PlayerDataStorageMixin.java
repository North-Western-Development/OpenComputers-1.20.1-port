package li.cil.oc.common.mixin;

import li.cil.oc.common.event.NanomachinesHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Replaces Forge's PlayerEvent.SaveToFile / LoadFromFile for the nanomachine state
 * (stored next to the player data as {@code <uuid>.ocnm}).
 */
@Mixin(PlayerDataStorage.class)
public abstract class PlayerDataStorageMixin {
    @Shadow
    @Final
    private File playerDir;

    @Inject(method = "save", at = @At("TAIL"))
    private void oc$onSave(Player player, CallbackInfo ci) {
        NanomachinesHandler.Common.onPlayerSave(player, playerDir);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void oc$onLoad(Player player, CallbackInfoReturnable<CompoundTag> cir) {
        NanomachinesHandler.Common.onPlayerLoad(player, playerDir);
    }
}
