package li.cil.oc.common.platform.fabric;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public final class ComponentPlatformImpl {
    private ComponentPlatformImpl() {
    }

    public static ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayer.get(level, profile);
    }

    public static boolean mayInteract(Player player, BlockPos pos, Direction face) {
        // Fabric has no pure permission event for block interaction; protection mods
        // hook UseBlockCallback, so ask it like Forge's RightClickBlock was asked.
        // TODO(port): listeners returning SUCCESS may perform their own interaction here.
        final BlockHitResult trace = new BlockHitResult(player.position(), face, pos, false);
        final InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, trace);
        return result != InteractionResult.FAIL;
    }

    public static boolean mayInteractWithEntity(Player player, Entity entity) {
        // Like for blocks: protection mods hook UseEntityCallback (null hit result = plain interact).
        // TODO(port): listeners returning SUCCESS may perform their own interaction here.
        final InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, entity, null);
        return result != InteractionResult.FAIL;
    }

    public static boolean canTossItem(ItemEntity item, Player player) {
        return true;
    }
}
