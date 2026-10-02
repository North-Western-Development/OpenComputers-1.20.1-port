package li.cil.oc.common.platform.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

public final class IntegrationPlatformImpl {
    private IntegrationPlatformImpl() {
    }

    public static ServerPlayer getFakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    public static boolean canUseBlock(Player player, BlockPos pos, Direction face) {
        final BlockHitResult trace = new BlockHitResult(player.position(), face, pos, false);
        final PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, trace);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getUseBlock() != Event.Result.DENY;
    }

    public static boolean canAttackBlock(Player player, BlockPos pos, Direction face) {
        final PlayerInteractEvent.LeftClickBlock event = ForgeHooks.onLeftClickBlock(player, pos, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        return !event.isCanceled() && event.getUseBlock() != Event.Result.DENY && event.getUseItem() != Event.Result.DENY;
    }
}
