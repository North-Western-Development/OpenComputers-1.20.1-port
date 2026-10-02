package li.cil.oc.common.platform.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import javax.annotation.Nullable;

public final class AgentPlatformImpl {
    private AgentPlatformImpl() {
    }

    public static boolean fireEntityInteract(Player player, Entity entity, InteractionHand hand) {
        return MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, hand, entity));
    }

    public static boolean fireRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hit) {
        final PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(player, hand, pos, hit);
        return event.isCanceled() || event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY;
    }

    public static boolean fireLeftClickBlock(Player player, BlockPos pos, Direction side) {
        final PlayerInteractEvent.LeftClickBlock event = ForgeHooks.onLeftClickBlock(player, pos, side, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        return event.isCanceled() || event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY;
    }

    public static boolean fireRightClickItem(Player player, InteractionHand hand) {
        final PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand);
        MinecraftForge.EVENT_BUS.post(event);
        return event.isCanceled() || event.getResult() == Event.Result.DENY;
    }

    public static void onPlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        ForgeEventFactory.onPlayerDestroyItem(player, stack, hand);
    }

    public static InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return stack.getItem().onItemUseFirst(stack, context);
    }

    public static boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return !stack.isEmpty() && stack.getItem().doesSneakBypassUse(stack, level, pos, player);
    }

    public static boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return state.canHarvestBlock(level, pos, player);
    }

    public static float getDigSpeed(Player player, BlockState state, BlockPos pos) {
        return player.getDigSpeed(state, pos);
    }

    private static boolean instantBreakListenerRegistered = false;

    public static synchronized void ensureInstantBreakSupport() {
        if (instantBreakListenerRegistered) return;
        instantBreakListenerRegistered = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerEvent.BreakSpeed.class, event -> {
            if (event.getEntity() instanceof li.cil.oc.server.agent.Player player && player.instantBreak) {
                event.setNewSpeed(Float.MAX_VALUE);
            }
        });
    }
}
