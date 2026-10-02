package li.cil.oc.common.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
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

/**
 * Loader specific hooks used by the agent (robot / drone) fake player,
 * {@code li.cil.oc.server.agent.Player}. Implemented in
 * {@code li.cil.oc.common.platform.forge.AgentPlatformImpl} and
 * {@code li.cil.oc.common.platform.fabric.AgentPlatformImpl}.
 * <p>
 * The {@code fire*} methods return {@code true} if the action should be
 * canceled (Forge: event canceled or relevant result DENY; Fabric: callback
 * returned anything but PASS).
 */
public final class AgentPlatform {
    private AgentPlatform() {
    }

    /** Forge {@code PlayerInteractEvent.EntityInteract}; Fabric {@code UseEntityCallback}. */
    @ExpectPlatform
    public static boolean fireEntityInteract(Player player, Entity entity, InteractionHand hand) {
        throw new AssertionError();
    }

    /** Forge {@code PlayerInteractEvent.RightClickBlock}; Fabric {@code UseBlockCallback}. */
    @ExpectPlatform
    public static boolean fireRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hit) {
        throw new AssertionError();
    }

    /** Forge {@code PlayerInteractEvent.LeftClickBlock}; Fabric {@code AttackBlockCallback}. */
    @ExpectPlatform
    public static boolean fireLeftClickBlock(Player player, BlockPos pos, Direction side) {
        throw new AssertionError();
    }

    /** Forge {@code PlayerInteractEvent.RightClickItem}; Fabric {@code UseItemCallback}. */
    @ExpectPlatform
    public static boolean fireRightClickItem(Player player, InteractionHand hand) {
        throw new AssertionError();
    }

    /** Forge {@code ForgeEventFactory.onPlayerDestroyItem}; no-op on Fabric. */
    @ExpectPlatform
    public static void onPlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        throw new AssertionError();
    }

    /** Forge {@code IForgeItem.onItemUseFirst}; {@code PASS} on Fabric. */
    @ExpectPlatform
    public static InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        throw new AssertionError();
    }

    /** Forge {@code IForgeItem.doesSneakBypassUse}; {@code false} on Fabric. */
    @ExpectPlatform
    public static boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        throw new AssertionError();
    }

    /** Forge {@code IForgeBlockState.canHarvestBlock}; vanilla tool check on Fabric. */
    @ExpectPlatform
    public static boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        throw new AssertionError();
    }

    /** Forge {@code Player.getDigSpeed(state, pos)} (fires BreakSpeed); vanilla {@code getDestroySpeed} on Fabric. */
    @ExpectPlatform
    public static float getDigSpeed(Player player, BlockState state, BlockPos pos) {
        throw new AssertionError();
    }

    /**
     * Makes sure that {@code li.cil.oc.server.agent.Player#instantBreak} is
     * honoured by the platform's dig speed computation. On Fabric overriding
     * {@code Player#getDestroySpeed} suffices (no-op); on Forge this registers
     * a {@code PlayerEvent.BreakSpeed} listener once.
     */
    @ExpectPlatform
    public static void ensureInstantBreakSupport() {
        throw new AssertionError();
    }
}
