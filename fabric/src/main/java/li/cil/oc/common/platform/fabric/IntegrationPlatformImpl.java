package li.cil.oc.common.platform.fabric;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public final class IntegrationPlatformImpl {
    private IntegrationPlatformImpl() {
    }

    public static ServerPlayer getFakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayer.get(level, profile);
    }

    public static boolean canUseBlock(Player player, BlockPos pos, Direction face) {
        final BlockHitResult trace = new BlockHitResult(player.position(), face, pos, false);
        final InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, trace);
        return result != InteractionResult.FAIL;
    }

    public static boolean canAttackBlock(Player player, BlockPos pos, Direction face) {
        final InteractionResult result = AttackBlockCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, pos, face);
        return result != InteractionResult.FAIL;
    }
}
