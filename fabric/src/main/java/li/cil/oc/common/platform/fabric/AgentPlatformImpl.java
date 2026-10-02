package li.cil.oc.common.platform.fabric;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import javax.annotation.Nullable;

public final class AgentPlatformImpl {
    private AgentPlatformImpl() {
    }

    public static boolean fireEntityInteract(Player player, Entity entity, InteractionHand hand) {
        return UseEntityCallback.EVENT.invoker().interact(player, player.level(), hand, entity, null) != InteractionResult.PASS;
    }

    public static boolean fireRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hit) {
        return UseBlockCallback.EVENT.invoker().interact(player, player.level(), hand, hit) != InteractionResult.PASS;
    }

    public static boolean fireLeftClickBlock(Player player, BlockPos pos, Direction side) {
        return AttackBlockCallback.EVENT.invoker().interact(player, player.level(), InteractionHand.MAIN_HAND, pos, side) != InteractionResult.PASS;
    }

    public static boolean fireRightClickItem(Player player, InteractionHand hand) {
        return UseItemCallback.EVENT.invoker().interact(player, player.level(), hand).getResult() != InteractionResult.PASS;
    }

    public static void onPlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        // No equivalent event on Fabric.
    }

    public static InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return InteractionResult.PASS;
    }

    public static boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return false;
    }

    public static boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state);
    }

    public static float getDigSpeed(Player player, BlockState state, BlockPos pos) {
        return player.getDestroySpeed(state);
    }

    public static void ensureInstantBreakSupport() {
        // Player#getDestroySpeed is overridden by the agent player itself.
    }
}
