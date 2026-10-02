package li.cil.oc.common.item;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.common.platform.PlatformHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Replacements for Forge-only item hooks, implemented on Architectury's
 * {@code InteractionEvent.RIGHT_CLICK_BLOCK} (fires before the block is activated on
 * both loaders and both sides):
 * <ul>
 * <li>{@code IForgeItem.onItemUseFirst} → {@link SimpleItem#onItemUseFirst(ItemStack, Player, Level, BlockPos, Direction, float, float, float, InteractionHand)}</li>
 * <li>{@code IForgeItem.doesSneakBypassUse} → {@link SimpleItem#sneakBypassesUse}: when sneaking with such an
 * item, the block's {@code use} is invoked here (vanilla would skip it).</li>
 * </ul>
 * Registered once by {@code Items.init()}.
 */
public final class ItemEvents {
    private ItemEvents() {
    }

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        InteractionEvent.RIGHT_CLICK_BLOCK.register(ItemEvents::onRightClickBlock);
    }

    private static EventResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        final ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof SimpleItem item)) return EventResult.pass();
        final Level world = player.level();
        final BlockHitResult hit = hitFor(player, pos, face);
        final Vec3 location = hit.getLocation();
        final float hitX = (float) (location.x - pos.getX());
        final float hitY = (float) (location.y - pos.getY());
        final float hitZ = (float) (location.z - pos.getZ());

        final InteractionResult first = item.onItemUseFirst(stack, player, world, pos, face, hitX, hitY, hitZ, hand);
        if (first != InteractionResult.PASS) {
            return first.consumesAction() ? EventResult.interruptTrue() : EventResult.interruptFalse();
        }

        if (player.isSecondaryUseActive() && item.sneakBypassesUse(stack, world, pos, player)) {
            final BlockState state = world.getBlockState(pos);
            final InteractionResult result = state.use(world, player, hand, hit);
            if (result.consumesAction()) {
                return EventResult.interruptTrue();
            }
        }
        return EventResult.pass();
    }

    private static BlockHitResult hitFor(Player player, BlockPos pos, Direction face) {
        final HitResult pick = player.pick(PlatformHooks.getBlockReach(player), 1f, false);
        if (pick instanceof BlockHitResult blockHit && pick.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(pos)) {
            return blockHit;
        }
        // Fall back to the center of the clicked face.
        final Vec3 center = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(center, face, pos, false);
    }
}
