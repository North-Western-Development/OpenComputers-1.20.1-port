package li.cil.oc.common.item;

import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class Wrench extends SimpleItem implements li.cil.oc.api.internal.Wrench {
    public Wrench(Properties props) {
        super(props);
    }

    @Override
    public boolean sneakBypassesUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (world.isLoaded(pos) && world.mayInteract(player, pos)) {
            final BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof SimpleBlock block && block.rotateBlock(world, pos, side)) {
                state.neighborChanged(world, pos, Blocks.AIR, pos, false);
                player.swing(hand);
                return !world.isClientSide ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
            } else {
                // TODO(port): Forge's BlockState.rotate(LevelAccessor, BlockPos, Rotation) is gone; vanilla rotate(Rotation).
                final BlockState updated = state.rotate(Rotation.CLOCKWISE_90);
                if (updated != state) {
                    world.setBlock(pos, updated, 3);
                    player.swing(hand);
                    return !world.isClientSide ? InteractionResult.sidedSuccess(world.isClientSide) : InteractionResult.PASS;
                } else return super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand);
            }
        } else return super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public boolean useWrenchOnBlock(Player player, Level world, BlockPos pos, boolean simulate) {
        if (!simulate) player.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
