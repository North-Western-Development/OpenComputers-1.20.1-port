package li.cil.oc.common.platform.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

public final class ComponentPlatformImpl {
    private ComponentPlatformImpl() {
    }

    public static ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    public static boolean mayInteract(Player player, BlockPos pos, Direction face) {
        final BlockHitResult trace = new BlockHitResult(player.position(), face, pos, false);
        final PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, trace);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getUseBlock() != Event.Result.DENY;
    }

    public static boolean canTossItem(ItemEntity item, Player player) {
        final ItemTossEvent event = new ItemTossEvent(item, player);
        final boolean canceled = MinecraftForge.EVENT_BUS.post(event);
        final boolean denied = event.hasResult() && event.getResult() == Event.Result.DENY;
        return !canceled && !denied;
    }
}
